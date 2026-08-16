# P4 Recipe Time Audit Analysis

## Cross-Reference Results

### Issues Found
  ℹ️  Pulverizer time unexpected: handwritten/snow_ice.json = 80 (intentional: ice is easier to pulverize)
  ℹ️  Pulverizer time unexpected: handwritten/snow_ice_blue.json = 200 (intentional: blue ice is harder)
  → Both are hand-written edge cases with justified different timing. No change needed per "无依据保持原值" rule.
## Detailed Verification

| Category | Expected Time | Actual (handwritten) | Actual (datagen) | Status |
|----------|--------------|---------------------|------------------|--------|
| Compressor (base) | 100 | 100 | 100 | ✅ |
| Compressor (mould pack) | 100 | N/A | 100 | ✅ |
| Compressor (vanilla pack) | 100 | N/A | 100 | ✅ |
| Pulverizer ingot→dust | 100 | 100 | 100 | ✅ |
| Pulverizer ore→dust | 180 | 180 | 180 | ✅ |
| Pulverizer raw→dust | 120 | 120 | 120 | ✅ |
| Pulverizer raw_block→dust | 900 | 900 (indirect) | 900 | ✅ |
| Furnace smelting | 200 | 200 (DataGen only) | 200 | ✅ |
| Furnace blasting | 100 | 100 (DataGen only) | 100 | ✅ |
| Induction Furnace (handwritten) | 50-300 | 50-300 | N/A | ✅ |
| Psionicant (handwritten) | 100-300 | 100-300 | N/A | ✅ |
| Refiner (handwritten) | 50-200 | 50-200 | N/A | ✅ |

## Conclusion


Based on the audit:
1. ✅ All 320 recipe JSONs have valid time/cookingTime > 0 in ticks
2. ✅ No FE/t field found in any recipe
3. ✅ Handwritten and DataGen times are consistent
4. ✅ All times match plan specifications where applicable
5. ✅ No recipe time changes needed for P4-T2/T3

P4-T1 audit complete. Moving to Cable P4-M1/M2 fixes (interleaved with P4-T2/T3).