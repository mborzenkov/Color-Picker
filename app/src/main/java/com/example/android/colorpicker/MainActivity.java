package com.example.android.colorpicker;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    Resources res;
    int numberOfSquares;
    int hsvStep;
    int[] arrayOfColors;
    LinearLayout linearLayout;
    GradientDrawable coloredSquare;
    ActionBar mActionBar;

    /* На экране присутствуют:
     *
     *      элемент выбора цвета;
     *      элемент, показывающий текущий выбранный цвет.
     *
     * Как устроен элемент выбора цвета:
     *      scrollview;
     *      внутри scrollview по горизонтали расположены 16 квадратов на одинаковом расстоянии друг от друга;
     *      margin у квадратов не менее 25% от стороны квадрата;
     *      квадраты покрашены в разные цвета (см. далее);
     *      на экране в портретной ориентации помещается 3-4 квадрата, добираться до остальных надо с помощью горизонтального скролла;
     *      клик по квадрату = выбор текущего цвета;
     *      фон в scrollview цветовой градиент по шкале оттенков Hue
     *      цвет квадрата равен цвету центральной точки квадрата, приходящейся на цвет фона.
     *
     * Элемент показа текущего цвета:
     *      прямоугольник, покрашенный в текущий цвет;
     *      вывод текущего цвета в RGB и HSV.
     *
     * Базовая задача: реализовать такой компонент.
     */

    // TODO: Добавить избранные квадраты, сохранять туда историю
    // TODO: Добавить обработку длительного нажатия
    // TODO: Добавить изменение цвета квадрата при перетягивании влево-вправо
    // TODO: Добавить изменение цвета квадрата при перетягивании вверх-вниз
    // TODO: Добавить сброс цвета квадрата при дабл тапе
    // TODO: Поправить раскладку, чтобы красиво влазило на экран N с половиной квадратов
    // TODO: Добавить перераскладку под горизонтальный экран
    // TODO: Добавить возможность настройки квадратов и градиента start-end

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int curColor = ContextCompat.getColor(this, R.color.colorStartGradient);

        res = getResources();
        numberOfSquares = res.getInteger(R.integer.number_of_squares);

        hsvStep = countStep(curColor, ContextCompat.getColor(this, R.color.colorEndGradient), numberOfSquares);
        arrayOfColors = new int[numberOfSquares + 1];
        linearLayout = (LinearLayout) findViewById(R.id.linearLayout_main);
        mActionBar = getSupportActionBar();


        final float[] arrayOfPositions = new float[numberOfSquares + 1];
        final LayoutInflater layoutInflater = getLayoutInflater();

        float[] curColorHSV = new float[3];
        arrayOfColors[0] = curColor;
        arrayOfPositions[0] = 0;

        for (int i = 0; i < numberOfSquares; i++) {

            Color.colorToHSV(curColor, curColorHSV);

            curColorHSV[0] += hsvStep;
            coloredSquare = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.colored_square);
            coloredSquare.setColor(Color.HSVToColor(curColorHSV));
            curColorHSV[0] += hsvStep;
            curColor = Color.HSVToColor(curColorHSV);

            View square = layoutInflater.inflate(R.layout.square_list_item, null);
            View squareButton = square.findViewById(R.id.imageButton_colored_square);
            squareButton.setBackground(coloredSquare);
            squareButton.setTag(i);
            linearLayout.addView(square);

            arrayOfColors[i+1] = curColor;
            arrayOfPositions[i+1] = (float) i / (float) numberOfSquares;

        }

        coloredSquare = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.colored_square);
        coloredSquare.setColor(arrayOfColors[0]);
        findViewById(R.id.imageView_chosen).setBackground(coloredSquare);
        mActionBar.setBackgroundDrawable(new ColorDrawable(arrayOfColors[0]));

        ShapeDrawable.ShaderFactory shaderFactory = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                LinearGradient linearGradient = new LinearGradient(0, 0, width, height,
                        arrayOfColors,
                        arrayOfPositions,
                        Shader.TileMode.REPEAT);
                return linearGradient;
            }
        };
        PaintDrawable perfectGradient = new PaintDrawable();
        perfectGradient.setShape(new RectShape());
        perfectGradient.setShaderFactory(shaderFactory);

        linearLayout.setBackground(perfectGradient);

    }

    private int countStep(int startColor, int endColor, int numberOfSquares) {
        float[] startColorHSV = new float[3];
        float[] endColorHSV = new float[3];
        Color.colorToHSV(startColor, startColorHSV);
        Color.colorToHSV(endColor, endColorHSV);

        return Math.round((endColorHSV[0] - startColorHSV[0]) / (float) numberOfSquares);
    }

    public void clickOnSquare(View view) {

        final int position = (Integer) view.getTag();
        float[] colorOfSquareHSV = new float[3];
        Color.colorToHSV(arrayOfColors[position], colorOfSquareHSV);
        colorOfSquareHSV[0] += hsvStep;
        int colorOfSquare = Color.HSVToColor(colorOfSquareHSV);

        ((GradientDrawable) findViewById(R.id.imageView_chosen).getBackground()).setColor(colorOfSquare);
        mActionBar.setBackgroundDrawable(new ColorDrawable(colorOfSquare));
    }
}
