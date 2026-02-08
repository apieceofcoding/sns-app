#!/bin/bash

BASE_URL="http://localhost:8080"

# Create Post
echo "=== Create Post ==="
curl -X POST "${BASE_URL}/api/v1/posts" \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "content": "This is my first post!"
  }' \
  -v

echo -e "\n\n"

# Create Post with Media
echo "=== Create Post with Media ==="
curl -X POST "${BASE_URL}/api/v1/posts" \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "content": "This is my post with media!",
    "mediaIds": [1, 2]
  }' \
  -v

echo -e "\n\n"

# Get All Posts
echo "=== Get All Posts ==="
curl -X GET "${BASE_URL}/api/v1/posts" \
  -v

echo -e "\n\n"

# Get Post by ID
echo "=== Get Post by ID (ID=1) ==="
curl -X GET "${BASE_URL}/api/v1/posts/1" \
  -v

echo -e "\n\n"

# Update Post
echo "=== Update Post (ID=1) ==="
curl -X PUT "${BASE_URL}/api/v1/posts/1" \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "content": "This is my updated post!"
  }' \
  -v

echo -e "\n\n"

# Delete Post
echo "=== Delete Post (ID=1) ==="
curl -X DELETE "${BASE_URL}/api/v1/posts/1" \
  -b cookies.txt \
  -v

echo -e "\n"
