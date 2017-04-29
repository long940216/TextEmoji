package com.sctdroid.app.textemoji.emoji;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sctdroid.app.textemoji.R;
import com.sctdroid.app.textemoji.data.bean.ChatItem;
import com.sctdroid.app.textemoji.data.bean.Emoji;
import com.sctdroid.app.textemoji.data.bean.EmojiCategory;
import com.sctdroid.app.textemoji.data.bean.Me;
import com.sctdroid.app.textemoji.developer.DeveloperActivity;
import com.sctdroid.app.textemoji.me.MeActivity;
import com.sctdroid.app.textemoji.utils.Constants;
import com.sctdroid.app.textemoji.utils.EmojiUtils;
import com.sctdroid.app.textemoji.utils.EncoderUtils;
import com.sctdroid.app.textemoji.utils.SharePreferencesUtils;
import com.sctdroid.app.textemoji.utils.SingleFileScanner;
import com.sctdroid.app.textemoji.utils.SoftKeyboardUtils;
import com.sctdroid.app.textemoji.utils.TCAgentUtils;
import com.sctdroid.app.textemoji.utils.ToastUtils;
import com.sctdroid.app.textemoji.utils.WeixinShareUtils;
import com.sctdroid.app.textemoji.views.EmojiCategoryView;
import com.sctdroid.app.textemoji.views.EmojiTager;
import com.sctdroid.app.textemoji.views.TextEmoji;
import com.sctdroid.app.textemoji.views.adaptableviews.RadioAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lixindong on 4/18/17.
 */

public class EmojiFragment extends Fragment implements EmojiContract.View, BaseEmojiViewHolder.EventDelegate {
    private ContentAdapter mAdapter;
    private EmojiContract.Presenter mPresenter;

    /**
     * Views
     */
    private TextInputEditText mTextInputEditText;
    private RecyclerView mRecyclerView;
    private CardView mOptions;
    private ImageView mEmojiButton;
    private EmojiTager mEmojiTager;
    private EmojiRadioAdapter mEmojiRadioAdapter;
    private EmojiPagerAdapter mEmojiPagerAdapter;

    private int mTextSize;

    private boolean mWithShadow;
    private SingleFileScanner mScanner;
    private int mMinTextSize;
    private int mDefaultTextSize;
    private int mSpanPerSegment;

    /**
     * option type
     */
    private static final int OPTION_TYPE_NONE = -1;
    private static final int OPTION_TYPE_KEYBOARD = 0;
    private static final int OPTION_TYPE_OPTIONS = 1;
    private static final int OPTION_TYPE_EMOJI = 2;

    private int mType = OPTION_TYPE_NONE;


    @Override
    public void setPresenter(EmojiContract.Presenter presenter) {
        mPresenter = presenter;
    }

    public static EmojiFragment newInstance() {
        return new EmojiFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init adapter here
        mAdapter = new ContentAdapter(getActivity(), this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_emoji, container, false);

        // do initial things here
        initValues();
        initViews(root);
        initHeadBar(root);
        initRecyclerView(root);
        initEvent(root);
        initOptions(root);

        mPresenter.create();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.start();
    }

    private void initValues() {
        mMinTextSize = getResources().getInteger(R.integer.min_textSize);
        mSpanPerSegment = getResources().getInteger(R.integer.span_per_segment);
        mDefaultTextSize = getResources().getInteger(R.integer.option_default_textSize);
    }

    private void initOptions(View root) {
        mWithShadow = SharePreferencesUtils.withShadow(getActivity(), false);
        mTextSize = SharePreferencesUtils.textSize(getActivity(), mDefaultTextSize);

        SwitchCompat switchCompat = (SwitchCompat) root.findViewById(R.id.shadow_switch);
        switchCompat.setChecked(mWithShadow);

        SeekBar seekBar = (SeekBar) root.findViewById(R.id.text_size);
        seekBar.setProgress((mTextSize - mMinTextSize) / mSpanPerSegment);
    }

    private void initViews(View root) {
        mOptions = (CardView) root.findViewById(R.id.options);
        mEmojiTager = (EmojiTager) root.findViewById(R.id.emoji_tager);
        mEmojiRadioAdapter = new EmojiRadioAdapter(getActivity());
        mEmojiPagerAdapter = new EmojiPagerAdapter(getActivity());
        mEmojiTager.setRadioGroupAdapter(mEmojiRadioAdapter);
        mEmojiTager.setViewPagerAdapter(mEmojiPagerAdapter);

        mEmojiButton = (ImageView) root.findViewById(R.id.emoji_button);
    }

    private void initEvent(final View root) {
        final TextView sendButton = (TextView) root.findViewById(R.id.send_button);
        final ImageView switchButton = (ImageView) root.findViewById(R.id.switch_button);

        mTextInputEditText = (TextInputEditText) root.findViewById(R.id.text_input);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputText = mTextInputEditText.getText().toString();
                mPresenter.processInput(inputText, mTextSize, mWithShadow);
                if (!TextUtils.isEmpty(inputText)) {
                    TCAgentUtils.TextInput(getActivity(), inputText);
                }
            }
        });
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOptions.getVisibility() != View.VISIBLE) {
                    optionShowType(OPTION_TYPE_OPTIONS);
                } else {
                    optionShowType(OPTION_TYPE_KEYBOARD);
                }
            }
        });
        mEmojiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEmojiTager.getVisibility() != View.VISIBLE) {
                    optionShowType(OPTION_TYPE_EMOJI);
                } else {
                    optionShowType(OPTION_TYPE_KEYBOARD);
                }
            }
        });
        mTextInputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (mOptions.getVisibility() == View.VISIBLE) {
                        optionShowType(OPTION_TYPE_KEYBOARD);
                    }
                }
                scrollChatToBottom();
            }
        });
        mTextInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.getTrimmedLength(s) == 0) {
                    // hide send button, show switch button
                    sendButton.setVisibility(View.INVISIBLE);
                    switchButton.setVisibility(View.VISIBLE);
                } else {
                    // hide switch button, show send button
                    sendButton.setVisibility(View.VISIBLE);
                    switchButton.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mTextInputEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEmojiTager.getVisibility() == View.VISIBLE) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mTextInputEditText.getWindowToken(), 0);
                }
            }
        });
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                optionShowType(OPTION_TYPE_NONE);
                return false;
            }
        });

        SeekBar seekBar = (SeekBar) root.findViewById(R.id.text_size);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTextSize = progress * mSpanPerSegment + mMinTextSize;

                SharePreferencesUtils.applyTextSize(getActivity(), mTextSize);
                TCAgentUtils.UpdateTextSize(getActivity(), mTextSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        SwitchCompat switchCompat = (SwitchCompat) root.findViewById(R.id.shadow_switch);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mWithShadow = isChecked;

                SharePreferencesUtils.applyWithShadow(getActivity(), mWithShadow);
                TCAgentUtils.SwitchShadow(getActivity(), mWithShadow);
            }
        });

        // enter me
        ImageView rightOption = (ImageView) root.findViewById(R.id.right_option);
        rightOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), DeveloperActivity.class));
            }
        });

        mEmojiPagerAdapter.setOnItemClickListener(new EmojiCategoryView.ContentAdapter.OnItemClickListener() {
            @Override
            public void onItemClicked(View view, Emoji emoji) {
                int index = mTextInputEditText.getSelectionStart();
                Editable editable = mTextInputEditText.getText();
                editable.insert(index, emoji.emoji);
            }
        });

        mEmojiPagerAdapter.setOnDeleteClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = mTextInputEditText.getSelectionStart();
                if (index > 0) {
                    Editable editable = mTextInputEditText.getText();
                    int length = 1;
                    if (index > 1) {
                        String text = editable.toString().substring(index-2,index);
                        if (EmojiUtils.isEmoji(text)) {
                            length = 2;
                        }
                    }
                    editable.delete(index - length, index);
                }
            }
        });
    }

    private void initRecyclerView(View root) {
        mRecyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void initHeadBar(View root) {
        TextView title = (TextView) root.findViewById(R.id.title);
        ImageView left_option = (ImageView) root.findViewById(R.id.left_option);
        ImageView right_option = (ImageView) root.findViewById(R.id.right_option);
        title.setText(R.string.string_emoji);
        left_option.setVisibility(View.GONE);
    }

    private void scrollChatToBottom() {
        mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    private void optionShowType(int type) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        switch (type) {
            case OPTION_TYPE_NONE:
                // hide all
                // do not focus
                imm.hideSoftInputFromWindow(mTextInputEditText.getWindowToken(), 0);
                mOptions.setVisibility(View.GONE);
                mEmojiTager.setVisibility(View.GONE);
                mEmojiButton.setImageResource(R.drawable.option_emoji);
                mTextInputEditText.clearFocus();
                break;
            case OPTION_TYPE_KEYBOARD:
                // show keyboard only
                // focus on edit text
                mTextInputEditText.requestFocus();
                imm.showSoftInput(mTextInputEditText, InputMethodManager.SHOW_FORCED);
                mOptions.setVisibility(View.GONE);
                mEmojiTager.setVisibility(View.GONE);
                mEmojiButton.setImageResource(R.drawable.option_emoji);
                TCAgentUtils.OptionClicked(getActivity(), Constants.LABEL_OPTION_HIDE);
                break;
            case OPTION_TYPE_OPTIONS:
                // show options only
                // do not focus on edit text
                imm.hideSoftInputFromWindow(mTextInputEditText.getWindowToken(), 0);
                mOptions.setVisibility(View.VISIBLE);
                mEmojiTager.setVisibility(View.GONE);
                mEmojiButton.setImageResource(R.drawable.option_emoji);
                mTextInputEditText.clearFocus();
                TCAgentUtils.OptionClicked(getActivity(), Constants.LABEL_OPTION_SHOW);
                break;
            case OPTION_TYPE_EMOJI:
                // show emoji only
                // focus on edit text
                mTextInputEditText.requestFocus();
                imm.hideSoftInputFromWindow(mTextInputEditText.getWindowToken(), 0);
                mOptions.setVisibility(View.GONE);
                mEmojiTager.setVisibility(View.VISIBLE);
                mEmojiButton.setImageResource(R.drawable.option_keyboard);
                break;
        }
    }

    /**
     *
     * @param view
     * @param data
     * @return
     */
    @Override
    public boolean onContentLongClicked(@NonNull View view,@NonNull Object data) {
        showShareDialog(view, data);
        return true;
    }

    private void showShareDialog(@NonNull final View view, @NonNull final Object data) {
        AlertDialog shareDialog = new AlertDialog.Builder(getActivity())
                .setItems(new String[]{
                        getString(R.string.share_to_wechat_friends_as_emoji),
                        getString(R.string.save_to_gallery),
                        getString(R.string.save_to_gallery_no_alpha)},
                        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (view instanceof TextEmoji && data instanceof ChatItem) {
                    TextEmoji emojiView = (TextEmoji) view;
                    Bitmap bitmap = emojiView.getBitmap(which == 0 || which == 1);
                    ChatItem item = (ChatItem) data;

                    if (which == 0) {
                        // share to friends
                        WeixinShareUtils.shareImage(bitmap);
                        TCAgentUtils.Share(getActivity(), Constants.LABEL_FROM_EMOJI, item.content);
                    } else if (which == 1 || which == 2) {
                        // savg to gallery
                        String filename = EncoderUtils.encodeSHA1(item.content + System.currentTimeMillis()) + ".png";
                        String absolutePath = StorageHelper.DIR_GALLERY + filename;
                        mPresenter.saveBitmap(bitmap, filename, StorageHelper.DIR_GALLERY);

                        // toast for saved path and notify gallery to update
                        ToastUtils.show(getActivity(), getString(R.string.saved_toast_format, absolutePath), Toast.LENGTH_LONG);
                        scanPhoto(absolutePath);

                        TCAgentUtils.SaveToGallery(getActivity(), which == 1, item.content);
                    }
                }

            }}).create();
        shareDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        shareDialog.show();
    }

    @Override
    public boolean onAvatarClicked(View view) {
        startActivity(new Intent(getActivity(), MeActivity.class));
        return true;
    }

    // This method is deprecated as it is not working on Android N
    private void notifyGalleryToUpdate(Uri uri) {
        Intent intent = new Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(uri);
        getActivity().sendBroadcast(intent);
    }

    public void scanPhoto(final String imageFileName) {
        if (mScanner == null) {
            mScanner = new SingleFileScanner(getActivity().getApplicationContext());
        }
        mScanner.scan(imageFileName);
    }


    /**
     * show Chats
     * @param data chat data
     */
    @Override
    public void showChats(List<ChatItem> data) {
        mAdapter.updateData(data);
        scrollChatToBottom();
    }

    @Override
    public void scrollToTop() {
        mRecyclerView.scrollToPosition(0);
    }

    @Override
    public void showEmptyText() {
        Toast.makeText(getActivity(), R.string.empty_text, Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void clearEditText() {
        mTextInputEditText.getText().clear();
    }

    @Override
    public void updateMe(Me me) {
        mAdapter.updateProfile(me);
    }

    @Override
    public void initEmojiBoard(List<EmojiCategory> data) {
        mEmojiRadioAdapter.updateData(data);
        mEmojiPagerAdapter.updateData(data);
    }

    public boolean onBackPressed() {
        if (mOptions.getVisibility() == View.VISIBLE
                || mEmojiTager.getVisibility() == View.VISIBLE) {
            optionShowType(OPTION_TYPE_NONE);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Classes for RecyclerView
     */
    static class DefaultViewHolder extends BaseEmojiViewHolder {
        private final TextView item_content;
        private final ImageView item_avatar;
        private final TextEmoji item_text_emoji;

        public DefaultViewHolder(Context context, LayoutInflater inflater, ViewGroup parent) {
            super(context, inflater.inflate(R.layout.chat_item, parent, false));
            item_content = (TextView) itemView.findViewById(R.id.item_content);
            item_avatar = (ImageView) itemView.findViewById(R.id.item_avatar);
            item_text_emoji = (TextEmoji) itemView.findViewById(R.id.text_emoji);
        }

        @Override
        protected void bind(@NonNull final ChatItem item) {
            if (ChatItem.NULL.equals(item)) {
                return;
            }
            item_content.setText(item.content);
            if (item.textSize > 0) {
                item_content.setTextSize(TypedValue.COMPLEX_UNIT_SP, item.textSize);
            }

            item_text_emoji.setText(item);

            item_text_emoji.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mDelegate != null) {
                        mDelegate.onContentLongClicked(v, item);
                    } else {
                    }
                }
            });

            item_text_emoji.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mDelegate != null) {
                        return mDelegate.onContentLongClicked(v, item);
                    } else {
                        return false;
                    }
                }
            });

            item_avatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mDelegate != null) {
                        mDelegate.onAvatarClicked(v);
                    }
                }
            });
/*
            Glide.with(getContext())
                    .load(item.avatarResId)
                    .into(item_avatar);
*/
        }

        /**
         * tmp method to show profile
         */
        public void bindProfile(Me mMe) {
//            item_avatar.setImageURI(Uri.fromFile(new File(mMe.getAvatar())));
        }
        public void bindAvatar(Bitmap avatar) {
            item_avatar.setImageBitmap(avatar);
        }
    }

    static class ContentAdapter extends RecyclerView.Adapter<BaseEmojiViewHolder> {
        private final Context mContext;
        private List<ChatItem> mData = new ArrayList<>();
        private final BaseEmojiViewHolder.EventDelegate mDelegate;
        private Me mMe;
        private Bitmap mAvatar;

        public ContentAdapter(Context context, BaseEmojiViewHolder.EventDelegate delegate) {
            super();
            mContext = context;
            mDelegate = delegate;
        }
        public Context getContext() {
            return mContext;
        }

        @Override
        public BaseEmojiViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DefaultViewHolder(getContext(), LayoutInflater.from(getContext()), parent);
        }

        @Override
        public void onBindViewHolder(BaseEmojiViewHolder holder, int position) {
            if (!Me.NULL.equals(mMe) && holder instanceof DefaultViewHolder) {
                ((DefaultViewHolder) holder).bindProfile(mMe);
                ((DefaultViewHolder) holder).bindAvatar(mAvatar);
            }
            holder.bind(getItem(position));
            holder.setEventDelegate(mDelegate);
        }

        private ChatItem getItem(int position) {
            return getItemCount() > position ? mData.get(position) : ChatItem.NULL;
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public void updateData(List<ChatItem> list) {
            mData.clear();
            mData.addAll(list);
            notifyDataSetChanged();
        }

        public void appendData(ChatItem item) {
            if (!ChatItem.NULL.equals(item)) {
                mData.add(item);
                notifyDataSetChanged();
            }
        }

        public void appendData(List<ChatItem> list) {
            if (list != null && list.size() > 0) {
                mData.addAll(list);
                notifyDataSetChanged();
            }
        }

        /**
         * tmp methods to show Me profile
         * @param me
         */
        public void updateProfile(Me me) {
            mMe = me;
            mAvatar = BitmapFactory.decodeFile(me.getAvatar());
            notifyDataSetChanged();
        }
    }
}
