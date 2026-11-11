package forge.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import com.badlogic.gdx.Gdx;
import forge.Forge;

import java.util.Locale;

/**
 * Debug controller simulator for testing gamepad navigation on devices or emulators without a gamepad.
 * <p>
 * {@code adb shell am broadcast -a forge.app.PAD --es b A} presses one button (A B X Y L1 R1 L2 R2 UP DOWN LEFT RIGHT
 * START SELECT) through the same code path as a real controller; {@code --es b DUMP} logs the focusable widgets.
 * Only senders holding {@code android.permission.DUMP} (the adb shell, not regular apps) can deliver it.
 */
final class AndroidControllerHook {
    static final String ACTION = "forge.app.PAD";
    private static final String TAG = "ForgePad";

    private AndroidControllerHook() { }

    static BroadcastReceiver register(Context context) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                String name = intent.getStringExtra("b");
                if (name == null || Gdx.app == null) {
                    return;
                }
                if ("DUMP".equalsIgnoreCase(name)) {
                    Gdx.app.postRunnable(() -> Log.i(TAG, "[controller-hook] " + Forge.describeControllerFocus()));
                    return;
                }
                final Forge.PadButton button;
                try {
                    button = Forge.PadButton.valueOf(name.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "unknown button " + name);
                    return;
                }
                Gdx.app.postRunnable(() -> {
                    Forge.handlePadButton(button);
                    Gdx.graphics.requestRendering();
                });
            }
        };
        IntentFilter filter = new IntentFilter(ACTION);
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Manifest.permission.DUMP, null, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter, Manifest.permission.DUMP, null);
        }
        return receiver;
    }
}
