package com.example.android.colorpicker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
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

    /** Контекст приложения */
    private static final Context APP_CONTEXT = App.getContext();
    /** Ресурсы в контексте приложения */
    private static final Resources RESOURCES = APP_CONTEXT.getResources();
    /** Количество квадратов суммарно */
    private static final int NUMBER_OF_SQUARES = RESOURCES.getInteger(R.integer.number_of_squares);
    /** Количество любимых цветов суммарно */
    private static final int FAVORITES_MAX = RESOURCES.getInteger(R.integer.favorites_max);
    /** Цвет начала градиента */
    private static final int COLOR_GRADIENT_START = ContextCompat.getColor(APP_CONTEXT, R.color.colorStartGradient);
    /** Цвет конца градиента */
    private static final int COLOR_GRADIENT_END = ContextCompat.getColor(APP_CONTEXT, R.color.colorEndGradient);
    /** Шаг, на котором будут располагаться края мультиградиента, квадрат находится на STEP_HUE/2 от края градиента */
    private static final int STEP_HUE = countStep(COLOR_GRADIENT_START, COLOR_GRADIENT_END, NUMBER_OF_SQUARES);
    /** Ссылка на компонент вибратор */
    private static final Vibrator VIBRATOR = (Vibrator) APP_CONTEXT.getSystemService(VIBRATOR_SERVICE);
    /** Минимальное время между вибрациями */
    private static final int TIMEOUT_VIBRATE = 1000; // 1 сек
    /** Продолжительность вибрации */
    private static final int VIBRATE_LENGTH = 50; // 0.05 сек

    /** Последняя случившаяся вибрация */
    private static long lastVibrate = 0;

    // Элементы layout и помощники
    private ActionBar mActionBar;
    private LayoutInflater mLayoutInflater;
    private HorizontalScrollView mScrollView;
    private LinearLayout mSquaresLinearLayout;
    private LinearLayout mFavoritesLinearLayout;
    private GradientDrawable mSquareDrawable;
    private TextView mRGBValueTextView;
    private TextView mHSVValueTextView;

    private Handler mHandler;
    // -

    /** Скорость перетягивания по горизонтали */
    private int mDivHUE;
    /** Скорость перетягивания по вертикали */
    private int mDivVAL;

    /** Список цветов у квадратов по умолчанию в формате HSV  */
    private List<float[]> mSquareStandardColorsHSV = new ArrayList<>();
    /** Список текущих цветов у квадратов в формате HSV */
    private List<float[]> mSquareColorsHSV = new ArrayList<>();
    /** Список любимых цветов в формате Color */
    private int[] mFavoriteColors = new int[FAVORITES_MAX];
    /** Выбранный цвет в формате HSV */
    private float[] chosenColorHSV = new float[3];

    /** Признак режима редактирования */
    private boolean editingMode = false;
    /** Признак двойного клика */
    private boolean doubleClick = false;
    /** Последний квадрат, на который кликнули */
    private View lastClickedSquare = null; // TODO: View -> ImageButton
    /** Предыдущее положение нажатия по X */
    private int xDelta = 0;
    /** Предыдущее положение нажатия по Y */
    private int yDelta = 0;


    /* TODO: Чистка кода и рефакторинг
     *      @NonNull & @Nullable
     *      Tag проверка на null
     *      DRY на View favSquare = mFavoritesLinearLayout.getChildAt(i).findViewById(R.id.imageButton_favorite_color);     ((GradientDrawable) favSquare.getBackground()).setColor(color);
     *      методы принимают View - поменять на более конкретный тип
     *      Color.Transparent
     */

    // TODO: Обновить README.md

    /**
     * Считает шаг для HUE между краями мультиградиента
     * @param startColor Начальный цвет
     * @param endColor Конечный цвет
     *                 Требуется, чтобы HUE в endColor был > HUE в startColor
     * @param numberOfSquares Количество квадратов
     * @return Шаг step*numberOfSquares ~= hue(endColor) - hue(startColor)
     */
    private static int countStep(int startColor, int endColor, int numberOfSquares) {
        float[] startColorHSV = new float[3];
        float[] endColorHSV = new float[3];
        Color.colorToHSV(startColor, startColorHSV);
        Color.colorToHSV(endColor, endColorHSV);

        return (int) ((endColorHSV[0] - startColorHSV[0]) / (float) numberOfSquares);
    }

    /** Копирует цвет в формате HSV
     *
     * @param colorFrom Цвет в формате HSV,
     */
    private static float[] copyOfColor(@NonNull float[] colorFrom) {
        return Arrays.copyOf(colorFrom, 3);
    }

    /** Возвращает новый Drawable квадратик с прозрачным цветом
     *
     * @return Квадратик Drawable
     */
    private static GradientDrawable getSquareDrawable() {
        return (GradientDrawable) ContextCompat.getDrawable(APP_CONTEXT, R.drawable.colored_square);
    }

    /** Заставляет телефон вибрировать
     *
     */
    private static void vibrate() {
        if (System.currentTimeMillis() - lastVibrate > TIMEOUT_VIBRATE) {
            VIBRATOR.vibrate(VIBRATE_LENGTH);
            lastVibrate = System.currentTimeMillis();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация
        mActionBar = getSupportActionBar();
        mLayoutInflater = getLayoutInflater();
        mScrollView = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
        mSquaresLinearLayout = (LinearLayout) findViewById(R.id.linearLayout_main);
        mFavoritesLinearLayout = (LinearLayout) findViewById(R.id.linearLayout_favorites);
        mSquareDrawable = getSquareDrawable();
        mRGBValueTextView = (TextView) findViewById(R.id.textView_RGB_value);
        mHSVValueTextView = (TextView) findViewById(R.id.textView_HSV_value);
        mHandler = new Handler();

        // Скорость перетягивания зависит от дисплея
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        double wi = (double)width/(double)dm.xdpi;
        double hi = (double)height/(double)dm.ydpi;
        mDivHUE = (int) (wi * 10);
        mDivVAL = (int) (hi * 500);

        // Эти два массива нужны для создания мультиградиента
        final int[] mArrayOfGradient = new int[NUMBER_OF_SQUARES + 1];
        final float[] arrayOfPositions = new float[NUMBER_OF_SQUARES + 1];

        // Считаем начальный цвет
        int curColor = COLOR_GRADIENT_START;
        float[] curColorHSV = new float[3];
        Color.colorToHSV(curColor, curColorHSV);
        mArrayOfGradient[0] = curColor;
        arrayOfPositions[0] = 0;
        mSquareDrawable.setColor(curColor);

        // Заполняем квадратиками поле
        for (int i = 0; i < NUMBER_OF_SQUARES; i++) {

            // Создаем Drawable квадратик и запоминаем его цвет в mSquareColorsHSV
            curColorHSV[0] += STEP_HUE / 2;
            curColor = Color.HSVToColor(curColorHSV);
            mSquareDrawable = getSquareDrawable();
            mSquareDrawable.setColor(curColor);
            mSquareColorsHSV.add(copyOfColor(curColorHSV));
            curColorHSV[0] += STEP_HUE / 2;

            // Запоминаем правую границу градиента и ее положение
            curColor = Color.HSVToColor(curColorHSV);
            mArrayOfGradient[i+1] = curColor;
            arrayOfPositions[i+1] = (float) i / (float) NUMBER_OF_SQUARES;

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
        float [] chosenColorHSV = savedInstanceState != null ? savedInstanceState.getFloatArray(CHOSEN_KEY) : copyOfColor(mSquareColorsHSV.get(0));
        mSquareDrawable = getSquareDrawable();
        findViewById(R.id.imageView_chosen).setBackground(mSquareDrawable);
        changeMainColor(chosenColorHSV, true);

        // Создаем Favorites квадратики
        for (int i = 0; i < FAVORITES_MAX; i++) {

            mSquareDrawable = getSquareDrawable();
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
            mSquareStandardColorsHSV.add(copyOfColor(val));
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
                    mSquaresLinearLayout.getBackground().clearColorFilter();
                } else if (doubleClick && view.equals(lastClickedSquare)) {
                    reverseColor(view);
                    doubleClick = false;
                } else {
                    doubleClick = true;
                    lastClickedSquare = view;
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

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.imageButton_colored_square:
                if (!editingMode) {
                    editingMode = true;
                    mScrollView.requestDisallowInterceptTouchEvent(true);
                    int position = (Integer) v.getTag();
                    changeMainColor(mSquareColorsHSV.get(position), false);
                    mSquaresLinearLayout.getBackground().setColorFilter(Color.argb(25, 0, 0, 0), PorterDuff.Mode.DARKEN);
                    vibrate();
                }
                return false;
            case R.id.imageButton_favorite_color:
                int position = (Integer) v.getTag();
                setFavoriteColor(position, Color.HSVToColor(chosenColorHSV));
                vibrate();
                return true;
        }
        return false;
    }

    /** Обработчик нажатия на Favorites квадратик
     * При вызове меняет цвет у главного квадратика, надписей вокруг него и строки меню
     * @param view Квадратик
     */
    public void clickOnFavSquare(@NonNull View view) {
        final int position = (Integer) view.getTag();
        if (mFavoriteColors[position] != 0) {
            float[] colorOfSquare = new float[3];
            Color.colorToHSV(mFavoriteColors[position], colorOfSquare);
            changeMainColor(colorOfSquare, true);
        }
    }

    /** Класс для обработки двойного клика
     *
     */
    private class HandleClick implements Runnable {
        View view;
        HandleClick(View view) { this.view = view; }
        public void run() {
            if (doubleClick) {
                clickOnSquare(view);
                doubleClick = false;
            }
        }
    }

    /** Обработчик нажатия на квадратик
     * При вызове меняет цвет у главного квадратика, надписей вокруг него, строки меню и Favorites
     * @param view Квадратик
     */
    private void clickOnSquare(@NonNull View view) {
        final int position = (Integer) view.getTag();
        float[] colorOfSquare = mSquareColorsHSV.get(position);
        changeMainColor(colorOfSquare, true);
    }

    /** Меняет цвет у основного квадратика, надписей вокруг него и строки меню на переданный colorHSV
     *
     * @param colorHSV Цвет в HSV, float[] размерностью 3
     * @param saveAsChosen Признак, нужно ли сохранить цвет
     */
    private void changeMainColor(@NonNull float[] colorHSV, boolean saveAsChosen) {
        if (saveAsChosen) {
            chosenColorHSV = copyOfColor(colorHSV);
        }
        int color = Color.HSVToColor(colorHSV);
        ((GradientDrawable) findViewById(R.id.imageView_chosen).getBackground()).setColor(color);
        mActionBar.setBackgroundDrawable(new ColorDrawable(color));
        mRGBValueTextView.setText("#" + Color.red(color) + ", " + Color.green(color) + ", " + Color.blue(color));
        mHSVValueTextView.setText("" + String.format(Locale.US, "%.2f", colorHSV[0]) + ", " + String.format(Locale.US, "%.2f", colorHSV[1]) + ", " + String.format(Locale.US, "%.2f", colorHSV[2]));
    }

    /** Обрабатывает изменения цвета в квадратике
     *
     * @param view Квадратик
     * @param HUE Изменение HUE от стандартного значения
     * @param VAL Изменение VAL от стандартного значения
     */
    private void updateColor(@NonNull View view, float HUE, float VAL) {

        final int position = (Integer) view.getTag();
        float[] standardColorHSV = mSquareStandardColorsHSV.get(position);
        float[] currentColorHSV = mSquareColorsHSV.get(position);
        float leftBorderHUE = standardColorHSV[0];
        float rightBorderHUE = standardColorHSV[0];
        leftBorderHUE = Math.max(leftBorderHUE - STEP_HUE, 0);
        rightBorderHUE = Math.min(rightBorderHUE + STEP_HUE, 360);
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
    private void reverseColor(@NonNull View view) {
        final int position = (Integer) view.getTag();
        float[] standardColorHSV = mSquareStandardColorsHSV.get(position);
        float[] currentColorHSV;
        currentColorHSV = copyOfColor(standardColorHSV);
        mSquareColorsHSV.set(position, currentColorHSV);
        View square = mSquaresLinearLayout.getChildAt(position).findViewById(R.id.imageButton_colored_square);
        ((GradientDrawable) square.getBackground()).setColor(Color.HSVToColor(currentColorHSV));
    }

    /** Запоминает цвет в выбранном любимом квадратике
     *
     * @param position Позиция квадратика, в котором нужно запомнить цвет
     * @param color Цвет, который нужно запомнить
     */
    private void setFavoriteColor(int position, int color) {
        View favSquare = mFavoritesLinearLayout.getChildAt(position).findViewById(R.id.imageButton_favorite_color);
        ((GradientDrawable) favSquare.getBackground()).setColor(color);
        mFavoriteColors[position] = color;
    }
}
