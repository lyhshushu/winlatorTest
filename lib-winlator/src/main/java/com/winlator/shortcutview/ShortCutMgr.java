package com.winlator.shortcutview;

import android.app.Activity;

/**
 * Description: TODO.
 *
 * @author yh.liu
 * @date 2025年07月09日 11:13
 * <p>
 * Copyright (c) 2025年, 4399 Network CO.ltd. All Rights Reserved.
 */
public class ShortCutMgr {

    private ShortcutBtn gameShortcut = null;

    public void addShortCut(Activity activity) {
        if (gameShortcut == null) {
            gameShortcut = new ShortcutBtn(activity);
        }
        gameShortcut.addShortcutView(activity);
    }

}
