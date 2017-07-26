package vn.rta.ipcall.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;


/**
 * Created by GeniusDoan on 29/05/2017.
 */
public class EraseButton extends ImageView implements AddressAware, OnClickListener, OnLongClickListener, TextWatcher {

    private AddressText address;

    public EraseButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
        setOnLongClickListener(this);
    }

    public void onClick(View v) {
        if (address.getText() != null && address.getText().length() > 0) {
            int lBegin = address.getSelectionStart();
            if (lBegin == -1) {
                lBegin = address.getEditableText().length() - 1;
            }
            if (lBegin > 0) {
                address.getEditableText().delete(lBegin - 1, lBegin);
            }
        }
    }

    public boolean onLongClick(View v) {
        address.getEditableText().clear();
        return true;
    }

    public void setAddressWidget(AddressText view) {
        address = view;
        if (view != null) {
            view.addTextChangedListener(this);
            setEnabled(view.getText().length() > 0);
        }
    }


    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        setEnabled(s.length() > 0);
    }

}
