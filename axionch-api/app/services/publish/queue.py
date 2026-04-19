import asyncio
from typing import Dict, List
from sqlalchemy.orm import Session
from sqlalchemy import select

from app.db.models import Post, PlatformPost, PublishStatus
from app.services.publish.registry import registry
from app.db.session import SessionLocal

class PublishQueue:
    def __init__(self):
        self.queue = asyncio.Queue()
        self._worker_task = None

    async def add_post(self, post_id: int, account_ids: List[int]):
        await self.queue.put((post_id, account_ids))
        if self._worker_task is None or self._worker_task.done():
            self._worker_task = asyncio.create_task(self._worker())

    async def _worker(self):
        while not self.queue.empty():
            post_id, account_ids = await self.queue.get()
            await self._process_post(post_id, account_ids)
            self.queue.task_done()

    async def _process_post(self, post_id: int, account_ids: List[int]):
        db: Session = SessionLocal()
        try:
            # Refresh post status
            post = db.get(Post, post_id)
            if not post:
                return

            success_count = 0
            total_accounts = len(account_ids)

            # Get platform posts for this post
            stmt = select(PlatformPost).where(
                PlatformPost.post_id == post_id,
                PlatformPost.social_account_id.in_(account_ids)
            )
            platform_posts = db.execute(stmt).scalars().all()

            # Map platform posts by account_id for easy access
            pp_map = {pp.social_account_id: pp for pp in platform_posts}

            for account_id in account_ids:
                pp = pp_map.get(account_id)
                if not pp:
                    continue

                # Use the adapter to publish
                adapter = registry.get(pp.platform)

                # Simulate some async work/network latency
                await asyncio.sleep(1)

                publish_result = adapter.publish(post.body, post.image_url)

                # Update platform post status
                pp.publish_status = PublishStatus.SUCCESS if publish_result.success else PublishStatus.FAILED
                pp.remote_post_id = publish_result.remote_post_id
                pp.error_message = publish_result.error_message

                if publish_result.success:
                    success_count += 1

                db.commit()

            # Update main post status
            if success_count == total_accounts:
                post.status = PublishStatus.SUCCESS
            elif success_count == 0:
                post.status = PublishStatus.FAILED
            else:
                post.status = PublishStatus.PARTIAL

            db.commit()

        except Exception as e:
            print(f"Error processing post {post_id}: {e}")
        finally:
            db.close()

publish_queue = PublishQueue()
