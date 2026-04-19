
# CreatorHub Architecture Overview

Components:

Android App
→ REST API
→ Platform adapters

Flow:

User creates post
→ Backend stores canonical post
→ Backend generates platform publish jobs
→ Adapter posts to platform API
→ Results returned to app
