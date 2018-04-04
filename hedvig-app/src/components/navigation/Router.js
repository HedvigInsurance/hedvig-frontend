import React from 'react';
import { AsyncStorage, StatusBar } from 'react-native';
import { connect } from 'react-redux';
import { NavigationActions, addNavigationHelpers } from 'react-navigation';
import { createReduxBoundAddListener } from 'react-navigation-redux-helpers';

import BaseNavigator from './base-navigator/BaseNavigator';
import { SEEN_MARKETING_CAROUSEL_KEY, IS_VIEWING_OFFER } from '../../constants';
import { REDIRECTED_INITIAL_ROUTE } from '../../actions/router';

const ReduxBaseNavigator = ({ dispatch, nav, addListener }) => {
  return (
    <BaseNavigator
      navigation={addNavigationHelpers({
        dispatch: dispatch,
        state: nav,
        addListener,
      })}
    />
  );
};

const ConnectedReduxBaseNavigator = connect(({ nav }, ownProps) => ({
  ...ownProps,
  nav,
}))(ReduxBaseNavigator);

class BaseRouter extends React.Component {
  constructor(props) {
    super(props);

    // Hooking up react-navigation + redux
    this.addListener = createReduxBoundAddListener('root');
    this._doRedirection = this._doRedirection.bind(this);
  }

  async _doRedirection() {
    if (this.props.hasRedirected || !this.props.insurance || !this.props.insurance.status) {
      return;
    }

    if (['ACTIVE', 'INACTIVE'].includes(this.props.insurance.status)) {
      this.props.redirectToRoute({ routeName: 'HomeBase' });
    } else {
      let isViewingOffer = await AsyncStorage.getItem(IS_VIEWING_OFFER);

      let action;
      if (isViewingOffer) {
        action = NavigationActions.navigate({
          routeName: 'ChatModal',
          params: {
            link: { view: 'Offer' },
          },
        });
      }

      this.props.redirectToRoute({
        routeName: 'ChatBase',
        action,
      });
    }
  }

  async componentDidMount() {
    if (this.props.hasRedirected) return;

    let alreadySeenMarketingCarousel = await AsyncStorage.getItem(SEEN_MARKETING_CAROUSEL_KEY);

    if (!alreadySeenMarketingCarousel) {
      this.props.redirectToRoute({ routeName: 'Marketing' });
    }

    this._doRedirection();
  }

  componentDidUpdate() {
    this._doRedirection();
  }

  render() {
    return (
      <React.Fragment>
        {/* backgroundColor only applies to Android */}
        <StatusBar backgroundColor="white" />
        <ConnectedReduxBaseNavigator addListener={this.addListener} />
      </React.Fragment>
    );
  }
}

const mapStateToProps = ({ insurance, router }, ownProps) => {
  return {
    ...ownProps,
    insurance,
    hasRedirected: router.hasRedirected,
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    redirectToRoute: (options) => {
      dispatch({ type: REDIRECTED_INITIAL_ROUTE });
      return dispatch(
        NavigationActions.reset({
          index: 0,
          actions: [NavigationActions.navigate(options)],
        }),
      );
    },
  };
};

export const Router = connect(mapStateToProps, mapDispatchToProps)(BaseRouter);
