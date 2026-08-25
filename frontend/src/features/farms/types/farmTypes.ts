export type LandAreaUnit = 'ACRE' | 'HECTARE';
export type FarmStatus = 'ACTIVE' | 'INACTIVE';
export type CropSeason = 'KHARIF' | 'RABI' | 'ZAID';
export type CropCategory = 'CEREAL' | 'PULSE' | 'OILSEED' | 'VEGETABLE' | 'FRUIT' | 'SPICE' | 'FIBER' | 'CASH_CROP' | 'OTHER';

export interface FarmLocation {
  state: string;
  district: string;
  mandal: string;
  village: string;
  pincode?: string;
}

export interface Farm {
  id: string;
  ownerUid: string;
  name: string;
  location: FarmLocation;
  landArea: number;
  landAreaUnit: LandAreaUnit;
  status: FarmStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFarmInput {
  name: string;
  location: FarmLocation;
  landArea: number;
  landAreaUnit: LandAreaUnit;
  status?: FarmStatus;
}

export interface CropMaster {
  id: string;
  name: string;
  category: CropCategory;
  scientificName?: string | null;
  status: string;
}

export interface FarmCrop {
  id: string;
  farmId: string;
  ownerUid: string;
  cropId: string;
  crop?: CropMaster;
  season: CropSeason;
  status: FarmStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFarmCropInput {
  cropId: string;
  season: CropSeason;
  status?: FarmStatus;
}

export interface LocationMaster {
  id: string;
  state: string;
  district: string;
  mandal: string;
  village: string;
  pincode?: string;
}
