package cm.aptoide.pt.v8engine.social.presenter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import cm.aptoide.pt.actions.PermissionManager;
import cm.aptoide.pt.actions.PermissionService;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.v8engine.InstallManager;
import cm.aptoide.pt.v8engine.crashreports.CrashReport;
import cm.aptoide.pt.v8engine.presenter.Presenter;
import cm.aptoide.pt.v8engine.presenter.View;
import cm.aptoide.pt.v8engine.repository.StoreRepository;
import cm.aptoide.pt.v8engine.social.data.AggregatedRecommendation;
import cm.aptoide.pt.v8engine.social.data.AppUpdate;
import cm.aptoide.pt.v8engine.social.data.AppUpdateCardTouchEvent;
import cm.aptoide.pt.v8engine.social.data.CardTouchEvent;
import cm.aptoide.pt.v8engine.social.data.CardType;
import cm.aptoide.pt.v8engine.social.data.FollowStoreCardTouchEvent;
import cm.aptoide.pt.v8engine.social.data.Media;
import cm.aptoide.pt.v8engine.social.data.PopularApp;
import cm.aptoide.pt.v8engine.social.data.PopularAppTouchEvent;
import cm.aptoide.pt.v8engine.social.data.Post;
import cm.aptoide.pt.v8engine.social.data.RatedRecommendation;
import cm.aptoide.pt.v8engine.social.data.Recommendation;
import cm.aptoide.pt.v8engine.social.data.SocialHeaderCardTouchEvent;
import cm.aptoide.pt.v8engine.social.data.StoreAppCardTouchEvent;
import cm.aptoide.pt.v8engine.social.data.StoreCardTouchEvent;
import cm.aptoide.pt.v8engine.social.data.StoreLatestApps;
import cm.aptoide.pt.v8engine.social.data.Timeline;
import cm.aptoide.pt.v8engine.social.view.TimelineView;
import cm.aptoide.pt.v8engine.store.StoreCredentialsProviderImpl;
import cm.aptoide.pt.v8engine.store.StoreUtilsProxy;
import cm.aptoide.pt.v8engine.view.app.AppViewFragment;
import java.util.List;
import java.util.concurrent.TimeUnit;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by jdandrade on 31/05/2017.
 */

public class TimelinePresenter implements Presenter {

  private final TimelineView view;
  private final Timeline timeline;
  private final CrashReport crashReport;
  private final TimelineNavigation timelineNavigation;
  private final PermissionManager permissionManager;
  private final PermissionService permissionRequest;
  private final InstallManager installManager;
  private final StoreRepository storeRepository;
  private final StoreUtilsProxy storeUtilsProxy;
  private final StoreCredentialsProviderImpl storeCredentialsProvider;

  public TimelinePresenter(@NonNull TimelineView cardsView, @NonNull Timeline timeline,
      CrashReport crashReport, TimelineNavigation timelineNavigation,
      PermissionManager permissionManager, PermissionService permissionRequest,
      InstallManager installManager, StoreRepository storeRepository,
      StoreUtilsProxy storeUtilsProxy, StoreCredentialsProviderImpl storeCredentialsProvider) {
    this.view = cardsView;
    this.timeline = timeline;
    this.crashReport = crashReport;
    this.timelineNavigation = timelineNavigation;
    this.permissionManager = permissionManager;
    this.permissionRequest = permissionRequest;
    this.installManager = installManager;
    this.storeRepository = storeRepository;
    this.storeUtilsProxy = storeUtilsProxy;
    this.storeCredentialsProvider = storeCredentialsProvider;
  }

  @Override public void present() {
    showCardsOnCreate();

    refreshCardsOnPullToRefresh();

    handleCardClickOnHeaderEvents();

    handleCardClickOnBodyEvents();

    showMoreCardsOnBottomReached();

    showCardsOnRetry();
  }

  @Override public void saveState(Bundle state) {

  }

  @Override public void restoreState(Bundle state) {

  }

  private void showCardsOnRetry() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.retry())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(created -> view.showProgressIndicator())
        .flatMapSingle(retryClicked -> timeline.getCards())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(cards -> showCardsAndHideProgress(cards))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(cards -> {
        }, throwable -> {
          throwable.printStackTrace();
          view.showGenericError();
        });
  }

  private void showMoreCardsOnBottomReached() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(create -> view.reachesBottom()
            .debounce(300, TimeUnit.MILLISECONDS))
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(create -> view.showLoadMoreProgressIndicator())
        .flatMapSingle(bottomReached -> timeline.getNextCards())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(cards -> showMoreCardsAndHideLoadMoreProgress(cards))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(cards -> {
        }, throwable -> throwable.printStackTrace());
  }

  private void handleCardClickOnHeaderEvents() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.articleClicked())
        .filter(cardTouchEvent -> cardTouchEvent.getActionType()
            .equals(CardTouchEvent.Type.HEADER))
        .doOnNext(cardTouchEvent -> {
          if (cardTouchEvent.getCard()
              .getType()
              .equals(CardType.VIDEO) || cardTouchEvent.getCard()
              .getType()
              .equals(CardType.ARTICLE)) {
            ((Media) cardTouchEvent.getCard()).getPublisherLink()
                .launch();
          } else if (isSocialPost(cardTouchEvent)) {
            SocialHeaderCardTouchEvent socialHeaderCardTouchEvent =
                ((SocialHeaderCardTouchEvent) cardTouchEvent);
            navigateToStoreTimeline(socialHeaderCardTouchEvent);
          } else if (cardTouchEvent.getCard()
              .getType()
              .equals(CardType.STORE)) {
            StoreLatestApps card = ((StoreLatestApps) cardTouchEvent.getCard());
            timelineNavigation.navigateToStoreHome(card.getStoreName(), card.getStoreTheme());
          } else if (cardTouchEvent.getCard()
              .getType()
              .equals(CardType.UPDATE)) {
            AppUpdate card = ((AppUpdate) cardTouchEvent.getCard());
            timelineNavigation.navigateToStoreHome(card.getStoreName(), card.getStoreTheme());
          } else if (cardTouchEvent.getCard()
              .getType()
              .equals(CardType.POPULAR_APP)) {
            PopularAppTouchEvent popularAppTouchEvent = (PopularAppTouchEvent) cardTouchEvent;
            timelineNavigation.navigateToStoreTimeline(popularAppTouchEvent.getUserId(),
                popularAppTouchEvent.getStoreTheme());
          }
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(articleUrl -> {
        }, throwable -> {
          throwable.printStackTrace();
          crashReport.log(throwable);
        });
  }

  private void handleCardClickOnBodyEvents() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.articleClicked())
        .filter(cardTouchEvent -> cardTouchEvent.getActionType()
            .equals(CardTouchEvent.Type.BODY))
        .doOnNext(cardTouchEvent -> {
          if (isMediaPost(cardTouchEvent)) {
            ((Media) cardTouchEvent.getCard()).getMediaLink()
                .launch();
          } else if (cardTouchEvent.getCard()
              .getType()
              .equals(CardType.RECOMMENDATION)) {
            Recommendation card = (Recommendation) cardTouchEvent.getCard();
            timelineNavigation.navigateToAppView(card.getAppId(), card.getPackageName(),
                AppViewFragment.OpenType.OPEN_ONLY);
          } else if (cardTouchEvent.getCard()
              .getType()
              .equals(CardType.STORE) || cardTouchEvent.getCard()
              .getType()
              .equals(CardType.AGGREGATED_SOCIAL_STORE)) {
            navigateToAppView((StoreAppCardTouchEvent) cardTouchEvent);
          } else if (cardTouchEvent.getCard()
              .getType()
              .equals(CardType.SOCIAL_STORE)) {
            if (cardTouchEvent instanceof StoreAppCardTouchEvent) {
              navigateToAppView((StoreAppCardTouchEvent) cardTouchEvent);
            } else if (cardTouchEvent instanceof FollowStoreCardTouchEvent) {
              FollowStoreCardTouchEvent followStoreCardTouchEvent =
                  ((FollowStoreCardTouchEvent) cardTouchEvent);
              followStore(followStoreCardTouchEvent.getStoreId(),
                  followStoreCardTouchEvent.getStoreName());
            } else if (cardTouchEvent instanceof StoreCardTouchEvent) {
              StoreCardTouchEvent storeCardTouchEvent = (StoreCardTouchEvent) cardTouchEvent;
              timelineNavigation.navigateToStoreHome(storeCardTouchEvent.getStoreName(),
                  storeCardTouchEvent.getStoreTheme());
            }
          } else if (cardTouchEvent.getCard()
              .getType()
              .equals(CardType.UPDATE)) {
            permissionManager.requestExternalStoragePermission(permissionRequest)
                .flatMap(success -> {
                  if (installManager.showWarning()) {
                    view.showRootAccessDialog();
                  }
                  return timeline.updateApp(cardTouchEvent);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged(install -> install.getState())
                .doOnNext(install -> {
                  // TODO: 26/06/2017 get this logic out of here?  this is not working properly yet
                  ((AppUpdate) cardTouchEvent.getCard()).setInstallationStatus(install.getState());
                  view.updateInstallProgress(cardTouchEvent.getCard(),
                      ((AppUpdateCardTouchEvent) cardTouchEvent).getCardPosition());
                })
                .subscribe(downloadProgress -> {
                }, throwable -> Logger.d(this.getClass()
                    // TODO: 26/06/2017 error handling
                    .getName(), "error"));
          } else if (cardTouchEvent.getCard()
              .getType()
              .equals(CardType.POPULAR_APP)) {
            PopularApp card = (PopularApp) cardTouchEvent.getCard();
            timelineNavigation.navigateToAppView(card.getAppId(), card.getPackageName(),
                AppViewFragment.OpenType.OPEN_ONLY);
          } else if (cardTouchEvent.getCard()
              .getType()
              .equals(CardType.SOCIAL_RECOMMENDATION) || cardTouchEvent.getCard()
              .getType()
              .equals(CardType.SOCIAL_INSTALL)) {
            RatedRecommendation card = (RatedRecommendation) cardTouchEvent.getCard();
            timelineNavigation.navigateToAppView(card.getAppId(), card.getPackageName(),
                AppViewFragment.OpenType.OPEN_ONLY);
          } else if (cardTouchEvent.getCard()
              .getType()
              .equals(CardType.AGGREGATED_SOCIAL_INSTALL)) {
            AggregatedRecommendation card = (AggregatedRecommendation) cardTouchEvent.getCard();
            timelineNavigation.navigateToAppView(card.getAppId(), card.getPackageName(),
                AppViewFragment.OpenType.OPEN_ONLY);
          }
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(articleUrl -> {
        }, throwable -> {
          throwable.printStackTrace();
          crashReport.log(throwable);
        });
  }

  private void navigateToAppView(StoreAppCardTouchEvent cardTouchEvent) {
    StoreAppCardTouchEvent storeCardTouchEvent = cardTouchEvent;
    timelineNavigation.navigateToAppView(storeCardTouchEvent.getPackageName(),
        AppViewFragment.OpenType.OPEN_ONLY);
  }

  private void refreshCardsOnPullToRefresh() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> view.refreshes())
        .flatMapSingle(refresh -> timeline.getCards())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(cards -> showCardsAndHideRefresh(cards))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(cards -> {
        }, throwable -> {
          throwable.printStackTrace();
          view.showGenericError();
        });
  }

  private void showCardsOnCreate() {
    view.getLifecycle()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .filter(__ -> view.isNewRefresh())
        .doOnNext(created -> view.showProgressIndicator())
        .flatMapSingle(created -> timeline.getCards())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(cards -> showCardsAndHideProgress(cards))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(cards -> {
        }, throwable -> {
          throwable.printStackTrace();
          view.showGenericError();
        });
  }

  private void showMoreCardsAndHideLoadMoreProgress(List<Post> cards) {
    view.hideLoadMoreProgressIndicator();
    view.showMoreCards(cards);
  }

  private void showCardsAndHideProgress(List<Post> cards) {
    view.hideProgressIndicator();
    view.showCards(cards);
  }

  private void showCardsAndHideRefresh(List<Post> cards) {
    view.hideRefresh();
    view.showCards(cards);
  }

  private boolean isMediaPost(CardTouchEvent cardTouchEvent) {
    return cardTouchEvent.getCard()
        .getType()
        .equals(CardType.VIDEO) || cardTouchEvent.getCard()
        .getType()
        .equals(CardType.ARTICLE) || cardTouchEvent.getCard()
        .getType()
        .equals(CardType.SOCIAL_ARTICLE) || cardTouchEvent.getCard()
        .getType()
        .equals(CardType.SOCIAL_VIDEO) || cardTouchEvent.getCard()
        .getType()
        .equals(CardType.AGGREGATED_SOCIAL_ARTICLE) || cardTouchEvent.getCard()
        .getType()
        .equals(CardType.AGGREGATED_SOCIAL_VIDEO);
  }

  private boolean isSocialPost(CardTouchEvent cardTouchEvent) {
    return cardTouchEvent.getCard()
        .getType()
        .equals(CardType.SOCIAL_ARTICLE) || cardTouchEvent.getCard()
        .getType()
        .equals(CardType.SOCIAL_VIDEO) || cardTouchEvent.getCard()
        .getType()
        .equals(CardType.SOCIAL_STORE) || cardTouchEvent.getCard()
        .getType()
        .equals(CardType.SOCIAL_RECOMMENDATION) || cardTouchEvent.getCard()
        .getType()
        .equals(CardType.SOCIAL_INSTALL);
  }

  private void navigateToStoreTimeline(SocialHeaderCardTouchEvent socialHeaderCardTouchEvent) {
    if (socialHeaderCardTouchEvent.getStoreName() != null) {
      timelineNavigation.navigateToStoreTimeline(socialHeaderCardTouchEvent.getStoreName(),
          socialHeaderCardTouchEvent.getStoreTheme());
    } else {
      timelineNavigation.navigateToStoreTimeline(socialHeaderCardTouchEvent.getUserId(),
          socialHeaderCardTouchEvent.getStoreTheme());
    }
  }

  private void followStore(long storeId, String storeName) {
    storeRepository.isSubscribed(storeId)
        .first()
        .observeOn(AndroidSchedulers.mainThread())
        .compose(view.bindUntilEvent(View.LifecycleEvent.PAUSE))
        .subscribe(isSubscribed -> {
          if (isSubscribed) {
            storeUtilsProxy.unSubscribeStore(storeName, storeCredentialsProvider);
            view.showStoreUnsubscribedMessage(storeName);
          } else {
            storeUtilsProxy.subscribeStore(storeName);
            view.showStoreSubscribedMessage(storeName);
          }
        }, (throwable) -> throwable.printStackTrace());
  }
}
