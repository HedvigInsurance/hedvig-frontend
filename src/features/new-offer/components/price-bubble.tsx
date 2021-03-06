import * as React from 'react';
import { View, ViewProps, Text, Animated, Dimensions } from 'react-native';
import styled from '@sampettersson/primitives';
import { colors, fonts } from '@hedviginsurance/brand';
import { Sequence, Spring, Delay } from 'animated-react-native-components';
import { MonetaryAmountV2, Campaign, Incentive } from 'src/graphql/components';
import { TranslationsConsumer } from 'src/components/translations/consumer';
import { TranslationsPlaceholderConsumer } from 'src/components/translations/placeholder-consumer';

const AnimatedView = Animated.createAnimatedComponent<ViewProps>(View);

const LARGE_CIRCLE_SIZE = 180;
const SMALL_CIRCLE_SIZE = 125;

const getCircleSize = () => {
  const windowHeight = Dimensions.get('window').height;

  if (windowHeight < 700) {
    return SMALL_CIRCLE_SIZE;
  }

  return LARGE_CIRCLE_SIZE;
};

const Circle = styled(View)({
  height: getCircleSize(),
  width: getCircleSize(),
  borderRadius: getCircleSize() / 2,
  backgroundColor: colors.WHITE,
  alignItems: 'center',
  justifyContent: 'center',
  shadowColor: 'black',
  shadowOpacity: 0.05,
  shadowOffset: {
    width: 0,
    height: 2,
  },
});

const DiscountCircle = styled(Circle)({
  backgroundColor: colors.PINK,
  height: getCircleSize() * 0.54,
  width: getCircleSize() * 0.54,
  borderRadius: (getCircleSize() * 0.54) / 2,
  transform: [{ translateX: getCircleSize() * 0.8 }],
  position: 'absolute',
});

const DiscountText = styled(Text)(({ size }: { size: 'small' | 'large' }) => ({
  color: colors.WHITE,
  fontFamily: fonts.CIRCULAR,
  fontSize:
    getCircleSize() === LARGE_CIRCLE_SIZE
      ? size == 'small'
        ? 12
        : 14
      : size == 'small'
        ? 11
        : 12,
  textAlign: 'center',
  fontWeight: size == 'small' ? 'normal' : 'bold',
}));

const Price = styled(Text)({
  color: colors.BLACK,
  fontSize: getCircleSize() === LARGE_CIRCLE_SIZE ? 60 : 40,
  fontFamily: fonts.CIRCULAR,
});

const MonthlyLabel = styled(Text)({
  color: colors.BLACK,
  fontSize: getCircleSize() === LARGE_CIRCLE_SIZE ? 20 : 18,
  fontFamily: fonts.CIRCULAR,
});

const GrossPrice = styled(Text)({
  fontFamily: fonts.CIRCULAR,
  fontSize: 14,
  textDecorationLine: 'line-through',
  textDecorationStyle: 'solid',
});

const NetPrice = styled(Price)({
  color: colors.PINK,
});

interface PriceBubbleProps {
  price: MonetaryAmountV2;
  discountedPrice: MonetaryAmountV2;
  redeemedCampaign: Campaign;
}

const formatMonetaryAmount = (monetaryAmount: MonetaryAmountV2) =>
  Number(monetaryAmount.amount);

const DiscountBubble: React.SFC<{ incentive: Incentive }> = ({ incentive }) => {
  if ('quantity' in incentive) {
    return (
      <DiscountCircle>
        <TranslationsConsumer textKey="OFFER_SCREEN_FREE_MONTHS_BUBBLE_TITLE">
          {(text) => <DiscountText size="small">{text}</DiscountText>}
        </TranslationsConsumer>
        <TranslationsPlaceholderConsumer
          textKey="OFFER_SCREEN_FREE_MONTHS_BUBBLE"
          replacements={{ free_month: incentive.quantity }}
        >
          {(nodes) => <DiscountText size="large">{nodes}</DiscountText>}
        </TranslationsPlaceholderConsumer>
      </DiscountCircle>
    );
  } else {
    return (
      <DiscountCircle>
        <TranslationsConsumer textKey="OFFER_SCREEN_INVITED_BUBBLE">
          {(text) => <DiscountText size="large">{text}</DiscountText>}
        </TranslationsConsumer>
      </DiscountCircle>
    );
  }
};

export const PriceBubble: React.SFC<PriceBubbleProps> = ({
  price,
  discountedPrice,
  redeemedCampaign,
}) => (
  <Sequence>
    <Delay config={{ delay: 650 }} />
    <Spring
      config={{
        bounciness: 12,
      }}
      toValue={1}
      initialValue={0.5}
    >
      {(animatedValue) => (
        <AnimatedView
          style={{
            opacity: animatedValue.interpolate({
              inputRange: [0.5, 1],
              outputRange: [0, 1],
            }),
            transform: [{ scale: animatedValue }],
          }}
        >
          <Circle>
            {discountedPrice.amount !== price.amount ? (
              <>
                <GrossPrice>{formatMonetaryAmount(price)} kr/mån</GrossPrice>
                <NetPrice>{formatMonetaryAmount(discountedPrice)}</NetPrice>
              </>
            ) : (
              <Price>{formatMonetaryAmount(price)}</Price>
            )}
            <MonthlyLabel>kr/mån</MonthlyLabel>
          </Circle>
          {redeemedCampaign !== null && (
            <DiscountBubble incentive={redeemedCampaign!.incentive!} />
          )}
        </AnimatedView>
      )}
    </Spring>
  </Sequence>
);
