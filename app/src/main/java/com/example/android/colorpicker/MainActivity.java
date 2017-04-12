package com.example.android.colorpicker;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;;

public class MainActivity extends AppCompatActivity {

    Map<View, Integer> allSquares = new HashMap<>();

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
    
    // TODO: Вывести в ListView 16 одинаковых квадратов
    // TODO: Сделать так, чтобы квадраты были разные и раскладывались красиво по градиенту
    // TODO: Обработать нажатия на квадраты и сохранить нажатое значение в отдельный квадрат
    // TODO: Устанавливать фон приложения как выбранный цвет
    // TODO: Добавить избранные квадраты, сохранять туда историю
    // TODO: Добавить обработку длительного нажатия
    // TODO: Добавить изменение цвета квадрата при перетягивании влево-вправо
    // TODO: Добавить изменение цвета квадрата при перетягивании вверх-вниз
    // TODO: Добавить сброс цвета квадрата при дабл тапе
    // TODO: Поправить раскладку, чтобы красиво влазило на экран N с половиной квадратов
    // TODO: Добавить перераскладку под горизонтальный экран
    // TODO: Добавить возможность программной настройки (values) количества квадратов и градиента start-end

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
