package nz.co.makereti.chimera.service;

import android.os.IInterface;

public interface IInRangeCallback extends IInterface {
    void onStart();
    void onStop();
    void onWifiEnabled();
    void onSSIDFound();
    void onSSIDLost();
    void onDirectionFound(boolean direction);
}
