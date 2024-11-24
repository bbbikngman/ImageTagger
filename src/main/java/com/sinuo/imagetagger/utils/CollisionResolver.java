package com.sinuo.imagetagger.utils;

import android.graphics.Paint;
import java.util.ArrayList;
import java.util.List;

public class CollisionResolver {
    // smaller,stricter
    public static final float CollisionCoefficient = 0.4F;
    public static class Interval implements Comparable<Interval> {
        public final float start;
        public final float end;

        public Interval(float start, float end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public int compareTo(Interval other) {
            return Float.compare(this.start, other.start);
        }

        public boolean overlaps(Interval other) {
            return !(this.end < other.start || this.start > other.end);
        }
    }

    /**
     * 解决单个标签的位置碰撞
     * @param proposedY 建议的Y坐标
     * @param textHeight 文本总高度（包含所有行）
     * @param imageHeight 图片高度
     * @param occupiedIntervals 已经被占用的区间列表
     * @return 调整后的Y坐标
     */
    public static float resolveCollision(float proposedY, float textHeight, float imageHeight,
                                         List<Interval> occupiedIntervals) {
        // 文本区域上下各扩展60%作为保护区
        float protectionZone = textHeight * CollisionCoefficient;
        float halfTextHeight = textHeight / 2;

        // 创建当前文本的区间
        float intervalStart = proposedY - halfTextHeight - protectionZone;
        float intervalEnd = proposedY + halfTextHeight + protectionZone;
        Interval currentInterval = new Interval(intervalStart, intervalEnd);

        // 检查是否有碰撞
        boolean hasCollision = false;
        for (Interval occupied : occupiedIntervals) {
            if (currentInterval.overlaps(occupied)) {
                hasCollision = true;
                break;
            }
        }

        if (!hasCollision) {
            return proposedY; // 没有碰撞，直接返回原始位置
        }

        // 有碰撞，尝试向上和向下移动
        float step = textHeight;
        float upY = proposedY;
        float downY = proposedY;
        float minY = textHeight;
        float maxY = imageHeight - textHeight;

        while (true) {
            // 向上尝试
            upY -= step;
            if (upY >= minY) {
                currentInterval = new Interval(
                        upY - halfTextHeight - protectionZone,
                        upY + halfTextHeight + protectionZone
                );
                hasCollision = false;
                for (Interval occupied : occupiedIntervals) {
                    if (currentInterval.overlaps(occupied)) {
                        hasCollision = true;
                        break;
                    }
                }
                if (!hasCollision) {
                    return upY;
                }
            }

            // 向下尝试
            downY += step;
            if (downY <= maxY) {
                currentInterval = new Interval(
                        downY - halfTextHeight - protectionZone,
                        downY + halfTextHeight + protectionZone
                );
                hasCollision = false;
                for (Interval occupied : occupiedIntervals) {
                    if (currentInterval.overlaps(occupied)) {
                        hasCollision = true;
                        break;
                    }
                }
                if (!hasCollision) {
                    return downY;
                }
            }

            // 如果上下都超出范围，返回原始位置
            if (upY < minY && downY > maxY) {
                return proposedY;
            }
        }
    }

    /**
     * 根据给定的Y坐标和文本高度，计算占用区间
     */
    public static Interval calculateOccupiedInterval(float y, float textHeight) {
        float protectionZone = textHeight * CollisionCoefficient;
        float halfTextHeight = textHeight / 2;
        return new Interval(
                y - halfTextHeight - protectionZone,
                y + halfTextHeight + protectionZone
        );
    }
}