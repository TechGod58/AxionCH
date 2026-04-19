from app.db.models import Platform
from app.services.publish.base import PublishAdapter
from app.services.publish.instagram_adapter import InstagramAdapter
from app.services.publish.linkedin_adapter import LinkedInAdapter
from app.services.publish.mock_adapters import (
    MockXAdapter,
    MockInstagramAdapter,
    MockLinkedInAdapter,
)
from app.services.publish.x_adapter import XAdapter


class AdapterRegistry:
    def __init__(self) -> None:
        self._real_adapters: dict[Platform, PublishAdapter] = {
            Platform.X: XAdapter(),
            Platform.LINKEDIN: LinkedInAdapter(),
            Platform.INSTAGRAM: InstagramAdapter(),
        }
        self._mock_adapters: dict[Platform, PublishAdapter] = {
            Platform.X: MockXAdapter(),
            Platform.LINKEDIN: MockLinkedInAdapter(),
            Platform.INSTAGRAM: MockInstagramAdapter(),
        }

    def get(self, platform: Platform, *, prefer_mock: bool = False) -> PublishAdapter:
        if prefer_mock:
            return self._mock_adapters[platform]
        return self._real_adapters[platform]


registry = AdapterRegistry()
