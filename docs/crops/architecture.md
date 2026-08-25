# Crop Master Architecture — Module 05

## Overview
The Crop Master dataset provides a standardized taxonomy of agricultural commodities across India.

## Categories
- `CEREAL` (Rice/Paddy, Wheat, Maize)
- `PULSE` (Red Gram/Tur, Bengal Gram)
- `OILSEED` (Groundnut, Mustard)
- `VEGETABLE` (Tomato, Onion, Potato)
- `FRUIT` (Mango, Banana)
- `SPICE` (Red Chilli, Turmeric)
- `FIBER` (Cotton, Jute)
- `CASH_CROP` (Sugarcane, Tobacco)
- `OTHER`

## Read-Only Security
Normal Farmers can read crops from `/api/v1/crops` but cannot insert, update, or delete master crop definitions.
