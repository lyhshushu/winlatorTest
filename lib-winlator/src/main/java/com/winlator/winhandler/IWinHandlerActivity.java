package com.winlator.winhandler;

import com.winlator.widget.InputControlsView;
import com.winlator.widget.XServerView;
import com.winlator.xserver.XServer;

/**
 * Description: TODO.
 *
 * @author yh.liu
 * @date 2025年07月01日 15:11
 * <p>
 * Copyright (c) 2025年, 4399 Network CO.ltd. All Rights Reserved.
 */
public interface IWinHandlerActivity {
    public InputControlsView getInputControlsView();
    public XServer getXServer();
    public XServerView getXServerView();
}
