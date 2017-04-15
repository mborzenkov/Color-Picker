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
import android.os.Handler;
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
    private final String FAVORITES_KEY = "FAV";
    private final String CHOSEN_KEY = "CHOSEN";
    private final long QUALIFICATION_SPAN = 200;

    private Resources mResources;
    private ActionBar mActionBar;
    private LayoutInflater mLayoutInflater;
    private HorizontalScrollView mScrollView;
    private LinearLayout mSquaresLinearLayout;
    private LinearLayout mFavoritesLinearLayout;
    private GradientDrawable mSquareDrawable;
    private TextView mRGBValueTextView;
    private TextView mHSVValueTextView;
    private Vibrator mVibrator;
    private Handler mHandler = new Handler();

    private int mNumberOfSquares = 16;
    private int mFavoritesMax = 4;
    private int mStepHUE;
    private int mDivHUE;
    private int mDivVAL;

    private int[] mArrayOfGradient = new int[mNumberOfSquares + 1];
    private List<float[]> mSquareColorsHSV = new ArrayList<>();
    private List<float[]> mSquareStandardColorsHSV;
    private int[] mFavoriteColors = new int[mFavoritesMax];
    private float[] chosenColorHSV = new float[3];

    private boolean editingMode = false;
    private boolean doubleClick = false;
    private View lastView = null;
    private int xDelta = 0;
    private int yDelta = 0;
    private long timeout = 0;

    // TODO: Изменение цветовой палитры, чтобы все цвета были красивые и первым был красный

    /* TODO: Чистка кода и рефакторинг
     *      @NonNull & @Nullable, static переменные, константы, static методы,
     *      Tag проверка на null, chosenColorHSV сразу со значениями, Color.Transparent в layout,
     *      работа с цветами Arrays.copyOf в метод класса,
     *      DRY на View favSquare = mFavoritesLinearLayout.getChildAt(i).findViewById(R.id.imageButton_favorite_color);     ((GradientDrawable) favSquare.getBackground()).setColor(color);
     *      методы принимают View - поменять на более конкретный тип
     *      параметры вибрации в константы и поменьше
     */

    // TODO: Обновить README.md

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

        // Ставим цвет у главного квадратика и у приложения
        float[] chosenColorHSV = new float[3];
        chosenColorHSV = savedInstanceState != null ? savedInstanceState.getFloatArray(CHOSEN_KEY) : Arrays.copyOf(mSquareColorsHSV.get(0), 3);
        mSquareDrawable = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.colored_square);
        mSquareDrawable.setColor(Color.TRANSPARENT);
        findViewById(R.id.imageView_chosen).setBackground(mSquareDrawable);
        changeMainColor(chosenColorHSV, true);

        // Создаем Favorites квадратики с прозрачным цветом для начала
        for (int i = 0; i < mFavoritesMax; i++) {

            mSquareDrawable = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.colored_square);
            mSquareDrawable.setColor(Color.TRANSPARENT);
            View favSquare = mLayoutInflater.inflate(R.layout.favorites_list_item, mFavoritesLinearLayout, false);
            View squareButton = favSquare.findViewById(R.id.imageButton_favorite_color);
            squareButton.setOnLongClickListener(this);
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

        if (savedInstanceState != null) {
            mFavoriteColors = savedInstanceState.getIntArray(FAVORITES_KEY);
            for (int i = 0; i < mFavoriteColors.length; i++) {
                if (mFavoriteColors[i] != 0) setFavoriteColor(i, mFavoriteColors[i]);
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(FAVORITES_KEY, mFavoriteColors);
        outState.putFloatArray(CHOSEN_KEY, chosenColorHSV);
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
        changeMainColor(colorOfSquare, true);
    }

    /** Обработчик нажатия на Favorites квадратик
     * При вызове меняет цвет у главного квадратика, надписей вокруг него и строки меню
     * @param view Квадратик
     */
    public void clickOnFavSquare(View view) {
        final int position = (Integer) view.getTag();
        if (mFavoriteColors[position] != 0) {
            float[] colorOfSquare = new float[3];
            Color.colorToHSV(mFavoriteColors[position], colorOfSquare);
            changeMainColor(colorOfSquare, true);
        }
    }

    /** Меняет цвет у основного квадратика, надписей вокруг него и строки меню на переданный colorHSV
     *
     * @param colorHSV Цвет в HSV, float[] размерностью 3
     * @param saveAsChosen Признак, нужно ли сохранить цвет
     */
    private void changeMainColor(float[] colorHSV, boolean saveAsChosen) {
        if (saveAsChosen) {
            chosenColorHSV = Arrays.copyOf(colorHSV, 3);
        }
        int color = Color.HSVToColor(colorHSV);
        ((GradientDrawable) findViewById(R.id.imageView_chosen).getBackground()).setColor(color);
        mActionBar.setBackgroundDrawable(new ColorDrawable(color));
        mRGBValueTextView.setText("#" + Color.red(color) + ", " + Color.green(color) + ", " + Color.blue(color));
        mHSVValueTextView.setText("" + String.format(Locale.US, "%.2f", colorHSV[0]) + ", " + String.format(Locale.US, "%.2f", colorHSV[1]) + ", " + String.format(Locale.US, "%.2f", colorHSV[2]));
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // Получаем позицию эвента
        final int X = (int) event.getRawX();
        final int Y = (int) event.getRawY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // При нажатии, запоминаем куда нажали
                xDelta = X;
                yDelta = Y;
                return false;
            case MotionEvent.ACTION_UP:
                // При отпускании, если был режим редактирования, отключаем его; иначе обрабатываем клик
                if (editingMode) {
                    editingMode = false;
                    mScrollView.requestDisallowInterceptTouchEvent(false);
                    changeMainColor(chosenColorHSV, false);
                } else if (doubleClick && view.equals(lastView)) {
                    reverseColor(view);
                    doubleClick = false;
                } else {
                    //Log.i("CLICK", "SINGLE");
                    doubleClick = true;
                    lastView = view;
                    mHandler.postDelayed(new HandleClick(view), QUALIFICATION_SPAN);
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                // Передвижение работает только в режиме редактирования, меняем цвета
                if (editingMode) {
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

    class HandleClick implements Runnable {
        View view;
        HandleClick(View view) { this.view = view; }
        public void run() {
            if (doubleClick) {
                clickOnSquare(view);
                doubleClick = false;
            }
        }
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


        int dynamicColor = Color.HSVToColor(currentColorHSV);
        View square = mSquaresLinearLayout.getChildAt(position).findViewById(R.id.imageButton_colored_square);
        ((GradientDrawable) square.getBackground()).setColor(dynamicColor);

        // Обновляем основной параллельно
        changeMainColor(currentColorHSV, false);
    }

    /** Возвращает цвет квадратика по умолчанию
     *
     * @param view Квадратик
     */
    private void reverseColor(View view) {
        final int position = (Integer) view.getTag();
        float[] standardColorHSV = mSquareStandardColorsHSV.get(position);
        float[] currentColorHSV;
        currentColorHSV = Arrays.copyOf(standardColorHSV, 3);
        mSquareColorsHSV.set(position, currentColorHSV);
        View square = mSquaresLinearLayout.getChildAt(position).findViewById(R.id.imageButton_colored_square);
        ((GradientDrawable) square.getBackground()).setColor(Color.HSVToColor(currentColorHSV));
    }

    /** Заставляет телефон вибрировать с таймаутом 1 секунда
     *
     */
    private void vibrate() {
        if (System.currentTimeMillis() - timeout > 1000) {
            mVibrator.vibrate(100);
            timeout = System.currentTimeMillis();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.imageButton_colored_square:
                if (!editingMode) {
                    editingMode = true;
                    mScrollView.requestDisallowInterceptTouchEvent(true);
                    vibrate();
                }
                return false;
            case R.id.imageButton_favorite_color:
                Log.i("EVENT", "Long press on fav square " + v.getTag());
                int position = (Integer) v.getTag();
                setFavoriteColor(position, Color.HSVToColor(chosenColorHSV));
                vibrate();
                return true;
        }
        return false;
    }

    // TODO: Написать javadoc
    private void setFavoriteColor(int position, int color) {
        View favSquare = mFavoritesLinearLayout.getChildAt(position).findViewById(R.id.imageButton_favorite_color);
        ((GradientDrawable) favSquare.getBackground()).setColor(color);
        mFavoriteColors[position] = color;
    }
}
