/*
 * Copyright 2016 Andy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.luckyandyzhang.mentionedittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * MentionEditText adds some useful features for mention string(@xxxx), such as highlight,
 * intelligent deletion, intelligent selection and '@' input detection, etc.
 *
 * @author Andy
 */
public class MentionEditText extends AppCompatEditText {
    public static final String DEFAULT_METION_TAG = "@";
    public static final String DEFAULT_MENTION_PATTERN = "@[\\u4e00-\\u9fa5\\w\\-]+";

    private Map<String, Pattern> mPatternMap = new HashMap<>();
    private Runnable mAction;

    private int mMentionTextColor;
    private boolean mAutoFormat;

    private boolean mIsSelected;
    private Range mLastSelectedRange;
    private List<Range> mRangeArrayList;

    private OnMentionInputListener mOnMentionInputListener;

    public MentionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MentionEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new HackInputConnection(super.onCreateInputConnection(outAttrs), true, this);
    }

    @Override
    public void setText(final CharSequence text, BufferType type) {
        super.setText(text, type);
        //hack, put the cursor at the end of text after calling setText() method
        if (mAction == null) {
            mAction = new Runnable() {
                @Override
                public void run() {
                    setSelection(getText().length());
                }
            };
        }
        post(mAction);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (lengthAfter == 1 && !TextUtils.isEmpty(text)) {
            char mentionChar = text.toString().charAt(start);
            String mentionString = String.valueOf(mentionChar);
            for (String tag : mPatternMap.keySet()) {
                if (tag.equals(mentionString) && mOnMentionInputListener != null) {
                    mOnMentionInputListener.onMentionCharacterInput(tag);
                    return;
                }
            }
        }

        if (mAutoFormat) {
            colorMentionString();
        } else {
            mentionTextChanged(start, lengthBefore, lengthAfter);
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        //avoid infinite recursion after calling setSelection()
        if (mLastSelectedRange != null && mLastSelectedRange.isEqual(selStart, selEnd)) {
            return;
        }

        //if user cancel a selection of mention string, reset the state of 'mIsSelected'
        Range closestRange = getRangeOfClosestMentionString(selStart, selEnd);
        if (closestRange != null && closestRange.to == selEnd) {
            mIsSelected = false;
        }

        Range nearbyRange = getRangeOfNearbyMentionString(selStart, selEnd);
        //if there is no mention string nearby the cursor, just skip
        if (nearbyRange == null) {
            return;
        }

        //forbid cursor located in the mention string.
        if (selStart == selEnd) {
            setSelection(nearbyRange.getAnchorPosition(selStart));
        } else {
            if (selEnd < nearbyRange.to) {
                setSelection(selStart, nearbyRange.to);
            }
            if (selStart > nearbyRange.from) {
                setSelection(nearbyRange.from, selEnd);
            }
        }
    }

    public void addMentionString(int uid, String name) {
        addMentionString(uid, name, true);
    }

    public void addMentionString(int uid, String name, boolean charBefore) {
        Editable editable = getText();
        int start = getSelectionStart();
        int end = start + name.length();
        editable.insert(start, name + " ");
        if (charBefore) {
            start--;
        }
        editable.setSpan(new ForegroundColorSpan(mMentionTextColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mRangeArrayList.add(new Range(uid, name, start, end));
        setSelection(end + 1);
    }

    /**
     * set regularExpression by tag
     *
     * @param pattern regularExpression
     */
    public void setPattern(String tag, String pattern) {
        mPatternMap.clear();
        addPattern(tag, pattern);
    }

    /**
     * add regularExpression by tag
     * @param tag   set tag for regularExpression
     * @param pattern   regularExpression
     */
    public void addPattern(String tag, String pattern) {
        mPatternMap.put(tag, Pattern.compile(pattern));
    }

    /**
     * if autoformat is false, you should call {@link #addMentionString(int, String, boolean)} } to add mentionString
     */
    public void setAutoFormat(boolean mAutoFormat) {
        this.mAutoFormat = mAutoFormat;
    }

    /**
     * set highlight color of mention string
     *
     * @param color value from 'getResources().getColor()' or 'Color.parseColor()' etc.
     */
    public void setMentionTextColor(int color) {
        mMentionTextColor = color;
    }

    /**
     * get a list of mention string
     *
     * @param excludeMentionCharacter if true, return mention string with format like 'Andy' instead of "@Andy"
     * @return list of mention string
     */
    public List<String> getMentionList(boolean excludeMentionCharacter) {
        List<String> mentionList = new ArrayList<>();
        if (TextUtils.isEmpty(getText().toString())) {
            return mentionList;
        }
        for (String tag : mPatternMap.keySet()) {
            mentionList.addAll(getMentionList(tag, excludeMentionCharacter));
        }
        return mentionList;
    }

    /**
     * get a list of mention string by tag
     *
     * @param excludeMentionCharacter if true, return mention string with format like 'Andy' instead of "@Andy"
     * @return list of mention string
     */
    public List<String> getMentionList(String tag, boolean excludeMentionCharacter) {
        List<String> mentionList = new ArrayList<>();
        if (TextUtils.isEmpty(getText().toString()) || !mPatternMap.containsKey(tag)) {
            return mentionList;
        }
        Pattern pattern = mPatternMap.get(tag);
        Matcher matcher = pattern.matcher(getText().toString());
        while (matcher.find()) {
            String mentionText = matcher.group();
            //tailor the mention string, using the format likes 'Andy' instead of "@Andy"
            if (excludeMentionCharacter) {
                //careful! 'Andy#' will be the result of '#Andy#' here
                mentionText = mentionText.substring(1);
            }
            if (!mentionList.contains(mentionText)) {
                mentionList.add(mentionText);
            }
        }
        return mentionList;
    }

    public String formatMentionString(String format) {
        String text = getText().toString();
        if (mRangeArrayList.isEmpty()) {
            return text;
        }

        StringBuilder builder = new StringBuilder("");
        int lastRangeTo = 0;
        //sort by asc
        Collections.sort(mRangeArrayList);

        for (Range range : mRangeArrayList) {
            if (range.name == null) {
                continue;
            }
            String newChar = String.format(format, range.id, range.name);
            builder.append(text.substring(lastRangeTo, range.from));
            builder.append(newChar);
            lastRangeTo = range.to;
        }
        builder.append(text.substring(lastRangeTo, text.length()));
        return builder.toString();
    }

    /**
     * set listener for mention character('@')
     *
     * @param onMentionInputListener MentionEditText.OnMentionInputListener
     */
    public void setOnMentionInputListener(OnMentionInputListener onMentionInputListener) {
        mOnMentionInputListener = onMentionInputListener;
    }

    private void init(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.MentionEditText);
        mMentionTextColor = typedArray.getColor(R.styleable.MentionEditText_mentionColor, Color.RED);
        mAutoFormat = typedArray.getBoolean(R.styleable.MentionEditText_autoformat, false);
        typedArray.recycle();

        mRangeArrayList = new ArrayList<>(5);
        setPattern(DEFAULT_METION_TAG, DEFAULT_MENTION_PATTERN);
        mMentionTextColor = Color.RED;
        //disable suggestion
        setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }

    private void colorMentionString() {
        //reset state
        mIsSelected = false;
        if (mRangeArrayList != null) {
            mRangeArrayList.clear();
        }

        Editable spannableText = getText();
        if (spannableText == null || TextUtils.isEmpty(spannableText.toString())) {
            return;
        }

        //remove previous spans
        ForegroundColorSpan[] oldSpans = spannableText.getSpans(0, spannableText.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan oldSpan : oldSpans) {
            spannableText.removeSpan(oldSpan);
        }

        //find mention string and color it
        String text = spannableText.toString();
        for (Map.Entry<String, Pattern> entry : mPatternMap.entrySet()) {
            int lastMentionIndex = -1;
            Matcher matcher = entry.getValue().matcher(text);
            while (matcher.find()) {
                String mentionText = matcher.group();
                int start;
                if (lastMentionIndex != -1) {
                    start = text.indexOf(mentionText, lastMentionIndex);
                } else {
                    start = text.indexOf(mentionText);
                }
                int end = start + mentionText.length();
                spannableText.setSpan(new ForegroundColorSpan(mMentionTextColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                lastMentionIndex = end;
                //record all mention-string's position
                mRangeArrayList.add(new Range(start, end));
            }
        }
    }

    /**
     * rebuild the list
     */
    private void mentionTextChanged(int start, int count, int after) {
        Editable editable = getText();
        //return if add at the end
        if (start >= editable.length()) {
            return;
        }

        int end = start + count;
        int offset = after - count;

        //remove unused range
        //reset the ranges between start and end
        Iterator<Range> iterator = mRangeArrayList.iterator();
        while (iterator.hasNext()) {
            Range range = iterator.next();
            if (range.isWrapped(start, end)) {
                iterator.remove();
                continue;
            }

            if (range.from >= end) {
                range.setOffset(offset);
            }
        }
    }

    private Range getRangeOfClosestMentionString(int selStart, int selEnd) {
        if (mRangeArrayList == null) {
            return null;
        }
        for (Range range : mRangeArrayList) {
            if (range.contains(selStart, selEnd)) {
                return range;
            }
        }
        return null;
    }

    private Range getRangeOfNearbyMentionString(int selStart, int selEnd) {
        if (mRangeArrayList == null) {
            return null;
        }
        for (Range range : mRangeArrayList) {
            if (range.isWrappedBy(selStart, selEnd)) {
                return range;
            }
        }
        return null;
    }

    //handle the deletion action for mention string, such as '@test'
    private class HackInputConnection extends InputConnectionWrapper {
        private EditText editText;

        HackInputConnection(InputConnection target, boolean mutable, MentionEditText editText) {
            super(target, mutable);
            this.editText = editText;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                int selectionStart = editText.getSelectionStart();
                int selectionEnd = editText.getSelectionEnd();
                Range closestRange = getRangeOfClosestMentionString(selectionStart, selectionEnd);
                if (closestRange == null) {
                    mIsSelected = false;
                    return super.sendKeyEvent(event);
                }
                //if mention string has been selected or the cursor is at the beginning of mention string, just use default action(delete)
                if (mIsSelected || selectionStart == closestRange.from) {
                    mIsSelected = false;
                    return super.sendKeyEvent(event);
                } else {
                    //select the mention string
                    mIsSelected = true;
                    mLastSelectedRange = closestRange;
                    setSelection(closestRange.to, closestRange.from);
                }
                return true;
            }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            if (beforeLength == 1 && afterLength == 0) {
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }

    /**
     * Listener for '@' character
     */
    public interface OnMentionInputListener {
        /**
         * call when '@' character is inserted into EditText
         */
        void onMentionCharacterInput(String tag);
    }

}
