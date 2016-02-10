package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class RxBleRadioOperationDisconnect extends RxBleRadioOperation<Void> {

    private final RxBleGattCallback rxBleGattCallback;

    private final AtomicReference<BluetoothGatt> bluetoothGattAtomicReference;

    private final BluetoothManager bluetoothManager;

    public RxBleRadioOperationDisconnect(RxBleGattCallback rxBleGattCallback, AtomicReference<BluetoothGatt> bluetoothGattAtomicReference, BluetoothManager bluetoothManager) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGattAtomicReference = bluetoothGattAtomicReference;
        this.bluetoothManager = bluetoothManager;
    }

    @Override
    public void run() {

        //noinspection Convert2MethodRef
        Observable.just(bluetoothGattAtomicReference.get())
                .flatMap(bluetoothGatt ->
                        bluetoothGatt == null ? Observable.empty() : Observable.just(bluetoothGatt))
                .flatMap(bluetoothGatt ->
                        isDisconnected(bluetoothGatt) ? Observable.just(bluetoothGatt) : disconnect(bluetoothGatt))
                .doOnTerminate(() -> releaseRadio())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        bluetoothGatt -> bluetoothGatt.close(),
                        throwable1 -> onError(throwable1),
                        () -> onCompleted()
                );
    }

    private boolean isDisconnected(BluetoothGatt bluetoothGatt) {
        return bluetoothManager.getConnectionState(bluetoothGatt.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * TODO: [DS] 09.02.2016 This operation makes the radio to block until disconnection - maybe it would be better if it would not?
     * What would happen then if a consecutive call to BluetoothDevice.connectGatt() would be made? What BluetoothGatt would be returned?
     * 1. A completely fresh BluetoothGatt - would work with the current flow
     * 2. The same BluetoothGatt - in this situation we should probably cancel the pending BluetoothGatt.close() call
     */
    private Observable<BluetoothGatt> disconnect(BluetoothGatt bluetoothGatt) {
        return Observable.create(subscriber -> {
            //noinspection Convert2MethodRef
            rxBleGattCallback
                    .getOnConnectionStateChange()
                    .filter(rxBleConnectionState -> rxBleConnectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED)
                    .take(1)
                    .map(rxBleConnectionState -> bluetoothGatt)
                    .subscribe(subscriber);

            bluetoothGatt.disconnect();
        });
    }
}
