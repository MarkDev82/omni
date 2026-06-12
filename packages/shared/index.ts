export type ActionStatus = 'PENDING' | 'DELIVERED' | 'COMPLETED' | 'FAILED';
export type ActionType = 'LOCATE' | 'RING';

export interface DeviceState {
  deviceId: string;
  batteryLevel: number;
  isCharging: boolean;
  lat?: number;
  lng?: number;
  accuracyMeters?: number;
  updatedAt: string;
}

export interface FcmPushPayload {
  omni_action_id: string;
  omni_action_type: ActionType;
}
