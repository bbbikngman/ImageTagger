package com.sinuo.imagetagger.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import com.sinuo.imagetagger.TaggedObject;

public class DrawingUtils {
    private static final String TAG = "DrawingUtils";
    private static final int TEXT_SIZE_BASE = 40;
    private static final int TEXT_PADDING = 8;
    private static final int TEXT_BG_ALPHA = 180; // Semi-transparent background
    private static final float CORNER_RADIUS = 8f;
    private static final float MAX_TEXT_WIDTH_RATIO = 0.8f; // 80% of screen width

    public static class TextMeasurement {
        public final float width;
        public final float height;
        public final List<String> lines;

        public TextMeasurement(float width, float height, List<String> lines) {
            this.width = width;
            this.height = height;
            this.lines = lines;
        }
    }

    public static Bitmap drawTagsOnBitmap(Bitmap originalBitmap, List<TaggedObject> taggedObjects) {
        if (originalBitmap == null || taggedObjects == null || taggedObjects.isEmpty()) {
            Log.w(TAG, "Invalid input parameters");
            return originalBitmap;
        }

        // Get current bitmap dimensions
        int currentWidth = originalBitmap.getWidth();
        int currentHeight = originalBitmap.getHeight();

        // Create a mutable copy of the bitmap
        Bitmap workingBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(workingBitmap);

        // Initialize paints
        Paint textBgPaint = createTextBackgroundPaint();
        Paint textPaint = createTextPaint(currentWidth, currentHeight);

        // 维护已占用区间的列表
        List<CollisionResolver.Interval> occupiedIntervals = new ArrayList<>();

        // First measure all text to get dimensions
        List<TextMeasurement> measurements = new ArrayList<>();
        float maxTextWidth = currentWidth * MAX_TEXT_WIDTH_RATIO;

        for (TaggedObject tag : taggedObjects) {
            measurements.add(measureAndWrapText(tag.getCombinedName(), textPaint, maxTextWidth));
        }

        // Now draw all tags with the measured dimensions
        for (int i = 0; i < taggedObjects.size(); i++) {
            try {
                TaggedObject tag = taggedObjects.get(i);
                TextMeasurement measurement = measurements.get(i);

                // 计算初始Y位置
                float tagY = (float)(tag.getTagRatioY() * currentHeight);

                // 解决碰撞
                float adjustedY = CollisionResolver.resolveCollision(
                        tagY,
                        measurement.height,
                        currentHeight,
                        occupiedIntervals
                );

                // 绘制标签
                drawSingleTag(canvas, tag, measurement,
                        currentWidth, currentHeight, textBgPaint, textPaint, adjustedY);

                // 添加到已占用区间
                occupiedIntervals.add(CollisionResolver.calculateOccupiedInterval(
                        adjustedY, measurement.height));

            } catch (Exception e) {
                Log.e(TAG, "Error drawing tag: " + taggedObjects.get(i).getCombinedName(), e);
            }
        }

        return workingBitmap;
    }

    private static void drawSingleTag(Canvas canvas, TaggedObject tag,
                                      TextMeasurement textMeasurement,
                                      int currentWidth, int currentHeight,
                                      Paint textBgPaint, Paint textPaint,
                                      float adjustedY) {
        // Calculate tag position using ratios
        float tagX = (float)(tag.getTagRatioX() * currentWidth);

        // Calculate scaled padding based on image size
        float scaledPadding = Math.min(currentWidth, currentHeight) * 0.01f;
        float actualPadding = Math.max(TEXT_PADDING, scaledPadding);

        // Calculate text metrics for positioning
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float lineHeight = fontMetrics.bottom - fontMetrics.top;

        // Adjust position to center the text block on the tag point
        float textX = tagX - (textMeasurement.width / 2);

        // Keep text within bounds
        textX = Math.max(actualPadding,
                Math.min(textX, currentWidth - textMeasurement.width - actualPadding));

        // 重要修改：调整背景矩形的位置计算
        // adjustedY 现在表示文本框的底部位置
        float textTopY = adjustedY - textMeasurement.height;  // 文本顶部位置
        float textBottomY = adjustedY;  // 文本底部位置

        // 绘制背景矩形，确保完全覆盖所有文本行
        RectF textBackgroundRect = new RectF(
                textX - actualPadding,
                textTopY - actualPadding,  // 从文本顶部开始
                textX + textMeasurement.width + actualPadding,
                textBottomY + actualPadding  // 到文本底部结束
        );
        canvas.drawRoundRect(textBackgroundRect, CORNER_RADIUS, CORNER_RADIUS, textBgPaint);

        // 绘制文本，从底部开始向上绘制每一行
        float currentY = textBottomY - actualPadding;  // 从底部开始
        for (int i = textMeasurement.lines.size() - 1; i >= 0; i--) {
            String line = textMeasurement.lines.get(i);
            canvas.drawText(line, textX, currentY, textPaint);
            currentY -= lineHeight;  // 向上移动一行
        }
    }

    // 在测量文本时也需要确保我们得到准确的高度
    private static TextMeasurement measureAndWrapText(String text, Paint paint, float maxWidth) {
        List<String> lines = new ArrayList<>();
        float maxLineWidth = 0;
        float totalHeight = 0;

        // Split text into words
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        Rect textBounds = new Rect();

        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float lineHeight = fontMetrics.bottom - fontMetrics.top;

        for (String word : words) {
            String testLine = currentLine.length() > 0
                    ? currentLine + " " + word
                    : word;

            paint.getTextBounds(testLine, 0, testLine.length(), textBounds);

            if (textBounds.width() > maxWidth && currentLine.length() > 0) {
                // Add current line and start new line with current word
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
                maxLineWidth = Math.max(maxLineWidth, textBounds.width());
                totalHeight += lineHeight;  // 使用lineHeight而不是textBounds.height()
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        // Add the last line
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
            paint.getTextBounds(currentLine.toString(), 0, currentLine.length(), textBounds);
            maxLineWidth = Math.max(maxLineWidth, textBounds.width());
            totalHeight += lineHeight;  // 使用lineHeight而不是textBounds.height()
        }

        // 确保totalHeight至少是一行的高度
        if (totalHeight == 0) {
            totalHeight = lineHeight;
        }

        return new TextMeasurement(maxLineWidth, totalHeight, lines);
    }

    private static void drawSingleTag(Canvas canvas, TaggedObject tag,
                                      TextMeasurement textMeasurement,
                                      int currentWidth, int currentHeight,
                                      Paint textBgPaint, Paint textPaint) {
        // Calculate tag position using ratios
        float tagX = (float)(tag.getTagRatioX() * currentWidth);
        float tagY = (float)(tag.getTagRatioY() * currentHeight);

        // Calculate scaled padding based on image size
        float scaledPadding = Math.min(currentWidth, currentHeight) * 0.01f;
        float actualPadding = Math.max(TEXT_PADDING, scaledPadding);

        // Calculate text metrics for positioning
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float lineHeight = fontMetrics.bottom - fontMetrics.top;

        // Adjust position to center the text block on the tag point
        float textX = tagX - (textMeasurement.width / 2);
        float textY = tagY;

        // Keep text within bounds
        textX = Math.max(actualPadding,
                Math.min(textX, currentWidth - textMeasurement.width - actualPadding));
        textY = Math.max(textMeasurement.height + actualPadding,
                Math.min(textY, currentHeight - actualPadding));

        // Draw text background for all lines
        RectF textBackgroundRect = new RectF(
                textX - actualPadding,
                textY - textMeasurement.height - actualPadding,
                textX + textMeasurement.width + actualPadding,
                textY + actualPadding
        );
        canvas.drawRoundRect(textBackgroundRect, CORNER_RADIUS, CORNER_RADIUS, textBgPaint);

        // Draw each line of text
        float currentY = textY - actualPadding/2;
        for (int i = textMeasurement.lines.size() - 1; i >= 0; i--) {
            String line = textMeasurement.lines.get(i);
            canvas.drawText(line, textX, currentY, textPaint);
            currentY -= lineHeight; // Move up for next line
        }
    }

    private static Paint createTextBackgroundPaint() {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAlpha(TEXT_BG_ALPHA);
        paint.setAntiAlias(true);
        return paint;
    }

    private static Paint createTextPaint(int width, int height) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);

        // Scale text size based on image dimensions (2% of smaller dimension)
        float scaledTextSize = Math.min(width, height) * 0.02f;
        paint.setTextSize(Math.max(TEXT_SIZE_BASE, scaledTextSize));

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setAntiAlias(true);
        return paint;
    }
}