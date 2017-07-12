package cm.aptoide.pt.spotandshareapp;

import android.graphics.drawable.Drawable;
import cm.aptoide.pt.spotandshareandroid.transfermanager.Transfer;

/**
 * Created by filipe on 10-07-2017.
 */

public class TransferAppModel {

  private String appName;
  private String packageName;
  private String filePath;
  private Drawable appIcon;

  private Transfer.State transferState;
  private boolean finishedTransference;
  private String senderName;
  private int hashcode;

  public TransferAppModel(String appName, String packageName, String filePath, Drawable appIcon,
      Transfer.State transferState, String senderName, int hashcode) {
    this.appName = appName;
    this.packageName = packageName;
    this.filePath = filePath;
    this.appIcon = appIcon;
    this.transferState = transferState;
    this.senderName = senderName;
    this.hashcode = hashcode;
  }

  public String getAppName() {
    return appName;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getFilePath() {
    return filePath;
  }

  public Drawable getAppIcon() {
    return appIcon;
  }

  public Transfer.State getTransferState() {
    return transferState;
  }

  public String getSenderName() {
    return senderName;
  }

  public boolean isTransferenceFinished() {
    return finishedTransference;
  }

  public void setTransferenceFinished(boolean finishedTransference) {
    this.finishedTransference = finishedTransference;
  }

  public int getHashcode() {
    return hashcode;
  }
}
