import * as React from 'react';
import RNVideo from 'react-native-video';
import { View, TouchableWithoutFeedback } from 'react-native';
import styled from '@sampettersson/primitives';
import { colors } from '@hedviginsurance/brand';

import { UploadMutation } from './upload-mutation';
import { Presend } from './presend';

interface VideoProps {
  uri: string;
  onUpload: (key: string) => void;
}

const Padding = styled(View)({
  padding: 10,
  paddingRight: 0,
  height: 250,
  width: 250,
});

const BorderRadius = styled(View)({
  borderRadius: 10,
  overflow: 'hidden',
});

const VideoContainer = styled(RNVideo)({
  height: '100%',
  width: '100%',
  backgroundColor: colors.WHITE,
});

export const Video: React.SFC<VideoProps> = ({ uri, onUpload }) => (
  <UploadMutation>
    {(uploadFile, isUploading) => (
      <Padding>
        <BorderRadius>
          <Presend
            isUploading={isUploading}
            onPressSend={() => {
              uploadFile(uri).then((response) => {
                if (response instanceof Error) {
                } else {
                  onUpload(response.key);
                }
              });
            }}
          >
            {(showPresendOverlay) => (
              <TouchableWithoutFeedback
                onPress={() => {
                  showPresendOverlay();
                }}
              >
                <VideoContainer
                  source={{ uri }}
                  muted
                  resizeMode="cover"
                  repeat
                />
              </TouchableWithoutFeedback>
            )}
          </Presend>
        </BorderRadius>
      </Padding>
    )}
  </UploadMutation>
);
