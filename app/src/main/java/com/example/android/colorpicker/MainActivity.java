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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Resources res;
    int numberOfSquares;
    int favoritesMax;
    int hsvStep;
    int[] arrayOfGradient;
    LinearLayout linearLayoutSquares;
    LinearLayout linearLayoutFavorites;
    GradientDrawable coloredSquare;
    ActionBar mActionBar;
    List<Integer> squareColors = new ArrayList<>();
    List<Integer> favoriteColors = new ArrayList<>();
    LayoutInflater mLayoutInflater;

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
    
    // TODO: Добавить изменение цвета квадрата при перетягивании влево-вправо
    // TODO: Добавить изменение цвета квадрата при перетягивании вверх-вниз
    // TODO: Добавить сброс цвета квадрата при дабл тапе
    // TODO: Поправить раскладку, чтобы красиво влазило на экран N с половиной квадратов
    // TODO: Добавить перераскладку под горизонтальный экран
    // TODO: Добавить возможность настройки квадратов и градиента start-end
    // TODO: Комментарии и оптимизация

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int curColor = ContextCompat.getColor(this, R.color.colorStartGradient);

        res = getResources();
        numberOfSquares = res.getInteger(R.integer.number_of_squares);
        favoritesMax = res.getInteger(R.integer.favorites_max);

        hsvStep = countStep(curColor, ContextCompat.getColor(this, R.color.colorEndGradient), numberOfSquares);
        arrayOfGradient = new int[numberOfSquares + 1];
        linearLayoutSquares = (LinearLayout) findViewById(R.id.linearLayout_main);
        linearLayoutFavorites = (LinearLayout) findViewById(R.id.linearLayout_favorites);
        mActionBar = getSupportActionBar();


        final float[] arrayOfPositions = new float[numberOfSquares + 1];
        mLayoutInflater = getLayoutInflater();

        float[] curColorHSV = new float[3];
        arrayOfGradient[0] = curColor;
        arrayOfPositions[0] = 0;

        for (int i = 0; i < numberOfSquares; i++) {

            Color.colorToHSV(curColor, curColorHSV);

            curColorHSV[0] += hsvStep;
            curColor = Color.HSVToColor(curColorHSV);
            coloredSquare = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.colored_square);
            coloredSquare.setColor(curColor);
            squareColors.add(curColor);
            curColorHSV[0] += hsvStep;
            curColor = Color.HSVToColor(curColorHSV);

            View square = mLayoutInflater.inflate(R.layout.square_list_item, linearLayoutSquares, false);
            View squareButton = square.findViewById(R.id.imageButton_colored_square);
            squareButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    longClick(v);
                    return true;
                }
            });
            squareButton.setBackground(coloredSquare);
            squareButton.setTag(i);
            linearLayoutSquares.addView(square);

            arrayOfGradient[i+1] = curColor;
            arrayOfPositions[i+1] = (float) i / (float) numberOfSquares;

        }

        coloredSquare = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.colored_square);
        coloredSquare.setColor(arrayOfGradient[0]);
        findViewById(R.id.imageView_chosen).setBackground(coloredSquare);

        mActionBar.setBackgroundDrawable(new ColorDrawable(arrayOfGradient[0]));

        for (int i = 0; i < favoritesMax; i++) {

            coloredSquare = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.colored_square);
            coloredSquare.setColor(Color.TRANSPARENT);
            View favSquare = mLayoutInflater.inflate(R.layout.favorites_list_item, linearLayoutFavorites, false);
            View squareButton = favSquare.findViewById(R.id.imageButton_favorite_color);
            squareButton.setBackground(coloredSquare);
            squareButton.setTag(i);
            linearLayoutFavorites.addView(favSquare);

        }

        ShapeDrawable.ShaderFactory shaderFactory = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                LinearGradient linearGradient = new LinearGradient(0, 0, width, height,
                        arrayOfGradient,
                        arrayOfPositions,
                        Shader.TileMode.REPEAT);
                return linearGradient;
            }
        };
        PaintDrawable perfectGradient = new PaintDrawable();
        perfectGradient.setShape(new RectShape());
        perfectGradient.setShaderFactory(shaderFactory);

        linearLayoutSquares.setBackground(perfectGradient);

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
        int colorOfSquare = squareColors.get(position);
        changeMainColor(colorOfSquare);
        addFavorite(colorOfSquare);
    }

    public void clickOnFavSquare(View view) {
        final int position = (Integer) view.getTag();
        if (position < favoriteColors.size()) {
            int colorOfSquare = favoriteColors.get(position);
            changeMainColor(colorOfSquare);
        }
    }

    private void changeMainColor(int color) {
        ((GradientDrawable) findViewById(R.id.imageView_chosen).getBackground()).setColor(color);
        mActionBar.setBackgroundDrawable(new ColorDrawable(color));
    }

    private void addFavorite(int color) {
        if (favoriteColors.indexOf(color) == -1) {

            if (favoriteColors.size() < favoritesMax) {
                favoriteColors.add(color);
            } else {
                favoriteColors.add(0, color);
                favoriteColors.remove(favoriteColors.size()-1);
            }

            for (int i = 0; i < favoriteColors.size(); i++) {
                View favSquare = linearLayoutFavorites.getChildAt(i).findViewById(R.id.imageButton_favorite_color);
                ((GradientDrawable) favSquare.getBackground()).setColor(favoriteColors.get(i));
                favSquare.setVisibility(View.VISIBLE);

            }
        }
    }

    private void longClick(View view) {

    }

}
