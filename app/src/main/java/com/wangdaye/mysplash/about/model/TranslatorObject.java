package com.wangdaye.mysplash.about.model;

import com.wangdaye.mysplash._common.i.model.AboutModel;

/**
 * Translator object.
 * */

public class TranslatorObject
        implements AboutModel {
    // data
    public int type = AboutModel.TYPE_TRANSLATOR;
    public String avatarUrl;
    public String title;
    public int flagId;
    public String subtitle;

    /** <br> life cycle. */

    public TranslatorObject(String avatarUrl, String title, int flagId, String subtitle) {
        this.avatarUrl = avatarUrl;
        this.title = title;
        this.flagId = flagId;
        this.subtitle = subtitle;
    }

    /** <br> model. */

    @Override
    public int getType() {
        return type;
    }
}
