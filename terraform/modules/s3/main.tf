variable "project_name" {}

resource "aws_s3_bucket" "chat_assets" {
  bucket = "${var.project_name}-chat-assets-${random_id.id.hex}"
  force_destroy = true
}

resource "random_id" "id" {
  byte_length = 4
}

resource "aws_s3_bucket_public_access_block" "public_access" {
  bucket = aws_s3_bucket.chat_assets.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

# Chính sách cho phép đọc file công khai (để mobile xem được ảnh)
resource "aws_s3_bucket_policy" "public_read" {
  bucket = aws_s3_bucket.chat_assets.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "s3:GetObject"
        Effect    = "Allow"
        Principal = "*"
        Resource  = "${aws_s3_bucket.chat_assets.arn}/*"
      },
    ]
  })
  depends_on = [aws_s3_bucket_public_access_block.public_access]
}

output "bucket_name" { value = aws_s3_bucket.chat_assets.id }
output "bucket_arn" { value = aws_s3_bucket.chat_assets.arn }
