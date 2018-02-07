import React from "react"
import { TabNavigator } from "react-navigation"
import styled from "styled-components/native"

import AssetList from "../../containers/asset-tracker/AssetList"
import Dashboard from "../../containers/dashboard/Dashboard"
import Profile from "../../containers/Profile"

import {
  StyledTabBarContainer,
  StyledTabBarButton,
  StyledTabBarButtonText
} from "../styles/tabbar"

const MyTabsContainer = styled.View`
  flex-direction: row;
  justify-content: space-between;
`

const TabBarButton = ({ title, disabled, navigation, navigateTo }) => {
  return (
    <StyledTabBarButton
      disabled={disabled}
      onPress={() => {
        navigation.navigate(navigateTo)
      }}
      activeOpacity={0.9}
    >
      <StyledTabBarButtonText disabled={disabled}>
        {title}
      </StyledTabBarButtonText>
    </StyledTabBarButton>
  )
}

class MyTabs extends React.Component {
  render() {
    return (
      <StyledTabBarContainer>
        <MyTabsContainer>
          <TabBarButton
            title="Försäkring"
            disabled={this.props.navigation.state.index === 0}
            navigation={this.props.navigation}
            navigateTo="DashboardTab"
          />
          <TabBarButton
            title="Prylbank"
            disabled={this.props.navigation.state.index === 1}
            navigation={this.props.navigation}
            navigateTo="AssetTrackerTab"
          />
          <TabBarButton
            title="Profil"
            disabled={this.props.navigation.state.index === 2}
            navigation={this.props.navigation}
            navigateTo="ProfileTab"
          />
        </MyTabsContainer>
      </StyledTabBarContainer>
    )
  }
}

const MyTabNavigator = TabNavigator(
  {
    DashboardTab: {
      screen: ({ navigation }) => (
        <Dashboard navigation={navigation} extraScrollViewPadding={80} />
      )
    },
    AssetTrackerTab: {
      screen: AssetList
    },
    ProfileTab: {
      screen: Profile
    }
  },
  {
    tabBarComponent: MyTabs,
    tabBarPosition: "top",
    swipeEnabled: true,
    animationEnabled: true,
    initialRouteName: "DashboardTab"
  }
)

export { MyTabNavigator }
