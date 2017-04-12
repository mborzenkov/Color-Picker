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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Vibrator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, View.OnLongClickListener {

    // Объявляем все переменные
    Resources mResources;
    ActionBar mActionBar;
    LayoutInflater mLayoutInflater;
    HorizontalScrollView mScrollView;
    LinearLayout mSquaresLinearLayout;
    LinearLayout mFavoritesLinearLayout;
    GradientDrawable mSquareDrawable;
    TextView mRGBValueTextView;
    TextView mHSVValueTextView;
    Vibrator mVibrator;

    int mNumberOfSquares;
    int mFavoritesMax;
    int mStepHUE;
    int mDivHUE;
    int mDivVAL;

    int[] mArrayOfGradient;
    List<float[]> mSquareColorsHSV = new ArrayList<>();
    List<float[]> mSquareStandardColorsHSV;
    List<float[]> mFavoriteColorsHSV = new ArrayList<>();

    boolean editingMode = false;
    int xDelta = 0;
    int yDelta = 0;
    long timeout = 0;

    /*
     * двойной клик по квадрату возвращает цвет в дефолтное значение;
     * оповещение пользователя о входе в режим редактирования;
     * оповещение пользователя о достижении граничных значений в режиме редактирования.
     * Оповещение пользователя может быть сделано через: вибрацию, звук, визуальное оповещение (см. сам)
     *
     * делаем longtap на квадрат с цветом;
     * не отпуская палец от экрана ведем влево или вправо:
     * горизонтальный скролл блокируется;
     * цвет в квадрате меняется в соответствующую сторону по шкале оттенков hue;
     * граница изменения — крайнее значение для первого и последнего квадратов или половина пути до соседнего квадрата;
     * отрываем палец от экрана — выходим из режима редактирования цвета;
     * bonus: не отрывая палец от экрана ведем вверх или вниз — меняем параметр V в модели HSV (он же Brightness);
     * bonus: в режиме редактирования может быть не виден квадрат с цветом, который мы редактируем (закрыт пальцем); подумать как решить эту проблему.
     * P.S. редактирование цвета в квадрате не меняет текущий выбранный цвет, чтобы выбрать цвет в квадрате — надо по нему кликнуть после редактирования
     *
     */

    // TODO: Добавить сброс цвета квадрата при дабл тапе
    // TODO: Поправить раскладку, чтобы красиво влазило на экран N с половиной квадратов
    // TODO: Добавить перераскладку под горизонтальный экран
    // TODO: Добавить возможность настройки квадратов и градиента start-end
    // TODO: Комментарии и оптимизация

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация
        mResources = getResources();
        mActionBar = getSupportActionBar();
        mLayoutInflater = getLayoutInflater();
        mScrollView = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
        mSquaresLinearLayout = (LinearLayout) findViewById(R.id.linearLayout_main);
        mFavoritesLinearLayout = (LinearLayout) findViewById(R.id.linearLayout_favorites);
        mSquareDrawable = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.colored_square);
        mRGBValueTextView = (TextView) findViewById(R.id.textView_RGB_value);
        mHSVValueTextView = (TextView) findViewById(R.id.textView_HSV_value);
        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        int curColor = ContextCompat.getColor(this, R.color.colorStartGradient);
        mNumberOfSquares = mResources.getInteger(R.integer.number_of_squares);
        mFavoritesMax = mResources.getInteger(R.integer.favorites_max);
        mStepHUE = countStep(curColor, ContextCompat.getColor(this, R.color.colorEndGradient), mNumberOfSquares);
        // mStepHUE - это шаг, на котором будут располагаться края мультиградиента, квадрат находится на mStepHUE/2 от края градиента
        // Шаг считается на основании начала градиента, конца и количества квадратов
        // Скорость перетягивания зависит от дисплея
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        double wi = (double)width/(double)dm.xdpi;
        double hi = (double)height/(double)dm.ydpi;
        mDivHUE = (int) (wi * 10);
        mDivVAL = (int) (hi * 500);

        Log.i("DISPLAY", "WI: " + wi + ", HI: " + hi);

        // Эти два массива нужны для создания мультиградиента
        mArrayOfGradient = new int[mNumberOfSquares + 1];
        final float[] arrayOfPositions = new float[mNumberOfSquares + 1];
        //- Инициализация


        // Считаем начальный цвет
        float[] curColorHSV = new float[3];
        Color.colorToHSV(curColor, curColorHSV);
        mArrayOfGradient[0] = curColor;
        arrayOfPositions[0] = 0;
        mSquareDrawable.setColor(curColor);

        // Заполняем квадратиками поле
        for (int i = 0; i < mNumberOfSquares; i++) {

            // Создаем Drawable квадратик и запоминаем его цвет в mSquareColorsHSV
            curColorHSV[0] += mStepHUE / 2;
            curColor = Color.HSVToColor(curColorHSV);
            mSquareDrawable = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.colored_square);
            mSquareDrawable.setColor(curColor);
            mSquareColorsHSV.add(Arrays.copyOf(curColorHSV, 3));
            curColorHSV[0] += mStepHUE / 2;

            // Запоминаем правую границу градиента и ее положение
            curColor = Color.HSVToColor(curColorHSV);
            mArrayOfGradient[i+1] = curColor;
            arrayOfPositions[i+1] = (float) i / (float) mNumberOfSquares;

            // Создаем View квадратик
            View square = mLayoutInflater.inflate(R.layout.square_list_item, mSquaresLinearLayout, false);
            View squareButton = square.findViewById(R.id.imageButton_colored_square);
            // Устанавливаем ему Listener'ы для действий с ним
            squareButton.setOnLongClickListener(this);
            squareButton.setOnTouchListener(this);
            squareButton.setBackground(mSquareDrawable);
            // Простой способ потом понимать какой квадратик какой - установить таги
            squareButton.setTag(i);
            mSquaresLinearLayout.addView(square);


            Color.colorToHSV(curColor, curColorHSV);

        }

        // Ставим цвет у главного квадратика и у приложения, как у первого квадратика
        mSquareDrawable = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.colored_square);
        mSquareDrawable.setColor(Color.TRANSPARENT);
        findViewById(R.id.imageView_chosen).setBackground(mSquareDrawable);
        changeMainColor(mSquareColorsHSV.get(0));

        // Создаем Favorites квадратики с прозрачным цветом для начала
        for (int i = 0; i < mFavoritesMax; i++) {

            mSquareDrawable = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.colored_square);
            mSquareDrawable.setColor(Color.TRANSPARENT);
            View favSquare = mLayoutInflater.inflate(R.layout.favorites_list_item, mFavoritesLinearLayout, false);
            View squareButton = favSquare.findViewById(R.id.imageButton_favorite_color);
            squareButton.setBackground(mSquareDrawable);
            squareButton.setTag(i);
            mFavoritesLinearLayout.addView(favSquare);

        }

        // Создаем мультиградиент и устанавливаем его
        ShapeDrawable.ShaderFactory shaderFactory = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                LinearGradient linearGradient = new LinearGradient(0, 0, width, height,
                        mArrayOfGradient,
                        arrayOfPositions,
                        Shader.TileMode.REPEAT);
                return linearGradient;
            }
        };
        PaintDrawable perfectGradient = new PaintDrawable();
        perfectGradient.setShape(new RectShape());
        perfectGradient.setShaderFactory(shaderFactory);
        mSquaresLinearLayout.setBackground(perfectGradient);

        // Запоминаем все начальные значения
        mSquareStandardColorsHSV = new ArrayList<>();
        for (float[] val : mSquareColorsHSV) {
            mSquareStandardColorsHSV.add(Arrays.copyOf(val, 3));
        }

    }

    /**
     * Считает шаг для HUE между краями мультиградиента
     * @param startColor Начальный цвет
     * @param endColor Конечный цвет
     *                 Требуется, чтобы HUE в endColor был > HUE в startColor
     * @param numberOfSquares Количество квадратов
     * @return Шаг step*numberOfSquares ~= hue(endColor) - hue(startColor)
     */
    private int countStep(int startColor, int endColor, int numberOfSquares) {
        float[] startColorHSV = new float[3];
        float[] endColorHSV = new float[3];
        Color.colorToHSV(startColor, startColorHSV);
        Color.colorToHSV(endColor, endColorHSV);

        return (int) ((endColorHSV[0] - startColorHSV[0]) / (float) numberOfSquares);
    }

    /** Обработчик нажатия на квадратик
     * При вызове меняет цвет у главного квадратика, надписей вокруг него, строки меню и Favorites
     * @param view Квадратик
     */
    private void clickOnSquare(View view) {
        final int position = (Integer) view.getTag();
        float[] colorOfSquare = mSquareColorsHSV.get(position);
        changeMainColor(colorOfSquare);
        addFavorite(colorOfSquare);
    }

    /** Обработчик нажатия на Favorites квадратик
     * При вызове меняет цвет у главного квадратика, надписей вокруг него и строки меню
     * @param view Квадратик
     */
    public void clickOnFavSquare(View view) {
        final int position = (Integer) view.getTag();
        if (position < mFavoriteColorsHSV.size()) {
            float[] colorOfSquare = mFavoriteColorsHSV.get(position);
            changeMainColor(colorOfSquare);
        }
    }

    /** Меняет цвет у основного квадратика, надписей вокруг него и строки меню на переданный colorHSV
     *
     * @param colorHSV Цвет в HSV, float[] размерностью 3
     */
    private void changeMainColor(float[] colorHSV) {
        int color = Color.HSVToColor(colorHSV);
        ((GradientDrawable) findViewById(R.id.imageView_chosen).getBackground()).setColor(color);
        mActionBar.setBackgroundDrawable(new ColorDrawable(color));
        mRGBValueTextView.setText("#" + Color.red(color) + ", " + Color.green(color) + ", " + Color.blue(color));
        mHSVValueTextView.setText("" + String.format(Locale.US, "%.2f", colorHSV[0]) + ", " + String.format(Locale.US, "%.2f", colorHSV[1]) + ", " + String.format(Locale.US, "%.2f", colorHSV[2]));
    }

    /** Добавляет цвет в избранное. Только если еще нет такого цвета. Заменяет последний, если уже заняты все слоты.
     *
     * @param colorHSV Цвет в HSV, float[] размерностью 3
     */
    private void addFavorite(float[] colorHSV) {
        if (mFavoriteColorsHSV.indexOf(colorHSV) == -1) {

            if (mFavoriteColorsHSV.size() < mFavoritesMax) {
                mFavoriteColorsHSV.add(colorHSV);
            } else {
                mFavoriteColorsHSV.add(0, colorHSV);
                mFavoriteColorsHSV.remove(mFavoriteColorsHSV.size()-1);
            }

            for (int i = 0; i < mFavoriteColorsHSV.size(); i++) {
                View favSquare = mFavoritesLinearLayout.getChildAt(i).findViewById(R.id.imageButton_favorite_color);
                ((GradientDrawable) favSquare.getBackground()).setColor(Color.HSVToColor(mFavoriteColorsHSV.get(i)));

            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // Получаем позицию эвента
        final int X = (int) event.getRawX();
        final int Y = (int) event.getRawY();
        FrameLayout.LayoutParams lParams;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // При нажатии, запоминаем куда нажали
                lParams = (FrameLayout.LayoutParams) mSquaresLinearLayout.getLayoutParams();
                xDelta = X;
                yDelta = Y;
                return false;
            case MotionEvent.ACTION_UP:
                // При отпускании, если был режим редактирования, отключаем его; иначе обрабатываем клик
                if (editingMode) {
                    editingMode = false;
                    mScrollView.requestDisallowInterceptTouchEvent(false);
                } else {
                    clickOnSquare(view);
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                // Передвижение работает только в режиме редактирования, меняем цвета
                if (editingMode) {
                    lParams = (FrameLayout.LayoutParams) mSquaresLinearLayout.getLayoutParams();
                    float HUE = ((float) (X - xDelta)) / mDivHUE;
                    float VAL = ((float) (Y - yDelta)) / mDivVAL;
                    updateColor(view, HUE, VAL);
                    xDelta = X;
                    yDelta = Y;
                }
                break;
        }
        mFavoritesLinearLayout.invalidate();
        return false;
    }

    /** Обрабатывает изменения цвета в квадратике
     *
     * @param view Квадратик
     * @param HUE Изменение HUE от стандартного значения
     * @param VAL Изменение VAL от стандартного значения
     */
    private void updateColor(View view, float HUE, float VAL) {

        final int position = (Integer) view.getTag();
        float[] standardColorHSV = mSquareStandardColorsHSV.get(position);
        float[] currentColorHSV = mSquareColorsHSV.get(position);
        float leftBorderHUE = Arrays.copyOf(standardColorHSV, 3)[0];
        float rightBorderHUE = Arrays.copyOf(standardColorHSV, 3)[0];
        leftBorderHUE -= mStepHUE;
        rightBorderHUE += mStepHUE;
        float topVAL = Math.min(standardColorHSV[2] + (standardColorHSV[2] / 4), 1);
        float bottomVAL = Math.max(standardColorHSV[2] - (standardColorHSV[2] / 4), 0);

        float changedHUE = currentColorHSV[0] + HUE;
        float changedVAL = currentColorHSV[2] - VAL;

        if (changedHUE < leftBorderHUE) {
            changedHUE = leftBorderHUE;
            vibrate();
        } else if (changedHUE > rightBorderHUE) {
            changedHUE = rightBorderHUE;
            vibrate();
        }

        if (changedVAL < bottomVAL) {
            changedVAL = bottomVAL;
            vibrate();
        } else if (changedVAL > topVAL) {
            changedVAL = topVAL;
            vibrate();
        }

        currentColorHSV[0] = changedHUE;
        currentColorHSV[2] = changedVAL;

        View square = mSquaresLinearLayout.getChildAt(position).findViewById(R.id.imageButton_colored_square);
        ((GradientDrawable) square.getBackground()).setColor(Color.HSVToColor(currentColorHSV));

    }

    @Override
    public boolean onLongClick(View v) {
        if (!editingMode) {
            editingMode = true;
            mScrollView.requestDisallowInterceptTouchEvent(true);
            vibrate();
        }
        return false;
    }

    private void vibrate() {
        if (System.currentTimeMillis() - timeout > 1000) {
            mVibrator.vibrate(100);
            timeout = System.currentTimeMillis();
        }
    }
}
