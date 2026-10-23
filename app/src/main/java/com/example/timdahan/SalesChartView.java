package com.example.timdahan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SalesChartView extends View {

    private List<DailySalesData> dataPoints = new ArrayList<>();
    private Paint barPaint;
    private Paint textPaint;
    private Paint axisPaint;
    private Paint amountPaint;
    private RectF barRect = new RectF();

    public SalesChartView(Context context) {
        super(context);
        init();
    }

    public SalesChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SalesChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#2563EB")); // TIMdahan Button Blue
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#6B7280"));
        textPaint.setTextSize(26f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        amountPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        amountPaint.setColor(Color.parseColor("#1F2937"));
        amountPaint.setTextSize(24f);
        amountPaint.setTextAlign(Paint.Align.CENTER);
        amountPaint.setFakeBoldText(true);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.parseColor("#E5E7EB"));
        axisPaint.setStrokeWidth(2f);
    }

    public void setDataPoints(List<DailySalesData> points) {
        this.dataPoints = (points != null) ? points : new ArrayList<>();
        invalidate(); // Redraw view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) return;

        float paddingBottom = 60f;
        float paddingTop = 40f;
        float paddingHorizontal = 40f;

        float chartHeight = height - paddingTop - paddingBottom;
        float chartBottom = height - paddingBottom;

        // Draw baseline
        canvas.drawLine(paddingHorizontal, chartBottom, width - paddingHorizontal, chartBottom, axisPaint);

        if (dataPoints.isEmpty()) {
            canvas.drawText("No sales data recorded for this period", width / 2f, height / 2f, textPaint);
            return;
        }

        // Find max sales value
        double maxSales = 0.0;
        for (DailySalesData p : dataPoints) {
            if (p.getTotalSales() > maxSales) {
                maxSales = p.getTotalSales();
            }
        }

        int count = dataPoints.size();
        float availableWidth = width - (2 * paddingHorizontal);
        float barWidth = Math.min(60f, (availableWidth / count) * 0.5f);
        float stepX = availableWidth / count;

        for (int i = 0; i < count; i++) {
            DailySalesData data = dataPoints.get(i);
            float cx = paddingHorizontal + (stepX * i) + (stepX / 2f);

            double sales = data.getTotalSales();
            float barHeight = 0f;
            if (maxSales > 0) {
                barHeight = (float) ((sales / maxSales) * (chartHeight - 30f));
            }

            // Draw Bar
            if (barHeight > 0) {
                float left = cx - (barWidth / 2f);
                float top = chartBottom - barHeight;
                float right = cx + (barWidth / 2f);
                float bottom = chartBottom;

                barRect.set(left, top, right, bottom);
                canvas.drawRoundRect(barRect, 12f, 12f, barPaint);

                // Draw Amount Text above bar
                String amountStr = String.format("₱%.0f", sales);
                canvas.drawText(amountStr, cx, top - 10f, amountPaint);
            }

            // Draw Date Label on X-Axis
            canvas.drawText(data.getDateLabel(), cx, chartBottom + 40f, textPaint);
        }
    }
}