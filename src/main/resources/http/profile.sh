#!/bin/bash

BASE_URL="http://localhost:8080"

# Get My Posts (including reposts)
echo "=== Get My Posts (including reposts) ==="
curl -X GET "${BASE_URL}/api/v1/profile/posts" \
  -b cookies.txt \
  -v

echo -e "\n\n"

# Get My Replies
echo "=== Get My Replies ==="
curl -X GET "${BASE_URL}/api/v1/profile/replies" \
  -b cookies.txt \
  -v

echo -e "\n\n"

# Get My Liked Posts
echo "=== Get My Liked Posts ==="
curl -X GET "${BASE_URL}/api/v1/profile/likes" \
  -b cookies.txt \
  -v

echo -e "\n\n"

# Init Profile Image Upload (requires authentication)
echo "=== Init Profile Image Upload ==="
curl -X POST "${BASE_URL}/api/v1/profile/image/init" \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "fileSize": 1024000
  }' \
  -v

echo -e "\n\n"

# Update Profile Image (requires authentication, after uploading to presigned URL)
echo "=== Update Profile Image ==="
curl -X PUT "${BASE_URL}/api/v1/profile/image" \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "mediaId": 1
  }' \
  -v

echo -e "\n"
