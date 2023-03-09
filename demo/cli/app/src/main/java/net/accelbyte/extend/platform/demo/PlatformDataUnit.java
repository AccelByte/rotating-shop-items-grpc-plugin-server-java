/*
 * Copyright (c) 2023 AccelByte Inc. All Rights Reserved
 * This is licensed software from AccelByte Inc, for limitations
 * and restrictions contact your company contract manager.
 */
package net.accelbyte.extend.platform.demo;

import net.accelbyte.extend.platform.demo.model.SimpleItemInfo;
import net.accelbyte.extend.platform.demo.model.SimpleSectionInfo;
import net.accelbyte.sdk.api.platform.models.*;
import net.accelbyte.sdk.api.platform.operations.catalog_changes.PublishAll;
import net.accelbyte.sdk.api.platform.operations.category.CreateCategory;
import net.accelbyte.sdk.api.platform.operations.item.CreateItem;
import net.accelbyte.sdk.api.platform.operations.section.CreateSection;
import net.accelbyte.sdk.api.platform.operations.section.PublicListActiveSections;
import net.accelbyte.sdk.api.platform.operations.section.UpdateSection;
import net.accelbyte.sdk.api.platform.operations.service_plugin_config.DeleteServicePluginConfig;
import net.accelbyte.sdk.api.platform.operations.service_plugin_config.UpdateServicePluginConfig;
import net.accelbyte.sdk.api.platform.operations.store.CreateStore;
import net.accelbyte.sdk.api.platform.operations.store.DeleteStore;
import net.accelbyte.sdk.api.platform.operations.store.ListStores;
import net.accelbyte.sdk.api.platform.operations.view.CreateView;
import net.accelbyte.sdk.api.platform.wrappers.*;
import net.accelbyte.sdk.core.AccelByteSDK;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PlatformDataUnit {
    private final AccelByteSDK abSdk;

    private final AppConfigRepository config;

    private final String abStoreName = "Item Rotation Plugin Demo Store";

    private final String abStoreDesc = "Description for item rotation grpc plugin demo store";

    private final String abViewName = "Item Rotation Default View";

    private final String abNamespace;

    private String storeId = "";

    private String viewId = "";

    public PlatformDataUnit(AccelByteSDK sdk, AppConfigRepository configRepo) throws Exception {
        abSdk = sdk;
        config = configRepo;
        abNamespace = configRepo.getNamespace();
    }

    protected String getRandomString(String characters, int length) {
        final Random random = new Random();
        final char[] result = new char[length];
        for (int i = 0; i < result.length; i++) {
            while (true) {
                result[i] = characters.charAt(random.nextInt(characters.length()));
                if (i > 0 && result[i - 1] == result[i])
                    continue;
                else break;
            }
        }
        return new String(result);
    }

    public void publishStoreChange() throws Exception {
        try {
            final PublishAll publishAllOp = PublishAll.builder()
                    .namespace(abNamespace)
                    .storeId(storeId)
                    .build();
            CatalogChanges wrapper = new CatalogChanges(abSdk);
            wrapper.publishAll(publishAllOp);
        } catch (Exception x) {
            System.out.println("Could not publish store changes. " + x.getMessage());
            throw x;
        }
    }

    public String createStore(boolean doPublish) throws Exception {

        final ListStores listStoresOp = ListStores.builder()
                .namespace(abNamespace)
                .build();

        Store storeWrapper = new Store(abSdk);
        final List<StoreInfo> stores = storeWrapper.listStores(listStoresOp);
        if ((stores != null) && (stores.size() > 0)) {
            //clean up draft stores
            for (StoreInfo store : stores) {
                if (!store.getPublished()) {
                    storeWrapper.deleteStore(DeleteStore.builder()
                            .namespace(abNamespace)
                            .storeId(store.getStoreId())
                            .build());
                }
            }
        }

        final List<String> sLangs = new ArrayList<>();
        sLangs.add("en");

        final List<String> sRegions = new ArrayList<>();
        sRegions.add("US");

        final StoreInfo newStore = storeWrapper.createStore(CreateStore.builder()
                .namespace(abNamespace)
                .body(StoreCreate.builder()
                        .title(abStoreName)
                        .description(abStoreDesc)
                        .defaultLanguage("en")
                        .defaultRegion("US")
                        .supportedLanguages(sLangs)
                        .supportedRegions(sRegions)
                        .build())
                .build());
        if (newStore == null)
            throw new Exception("Could not create new store.");
        storeId = newStore.getStoreId();

        if (doPublish)
            publishStoreChange();

        return storeId;
    }

    public void createCategory(String categoryPath, boolean doPublish) throws Exception {
        if (storeId.equals(""))
            throw new Exception("No store id stored.");

        Map<String,String> localz = new HashMap<>();
        localz.put("en",categoryPath);

        Category categoryWrapper = new Category(abSdk);
        categoryWrapper.createCategory(CreateCategory.builder()
                .namespace(abNamespace)
                .storeId(storeId)
                .body(CategoryCreate.builder()
                        .categoryPath(categoryPath)
                        .localizationDisplayNames(localz)
                        .build())
                .build());
    }

    public String createStoreView(boolean doPublish) throws Exception {
        if (storeId.equals(""))
            throw new Exception("No store id stored.");

        Map<String, Localization> localz = new HashMap<>();
        localz.put("en",Localization.builder()
                .title(abViewName)
                .build());

        View viewWrapper = new View(abSdk);
        FullViewInfo viewInfo = viewWrapper.createView(CreateView.builder()
                .namespace(abNamespace)
                .storeId(storeId)
                .body(ViewCreate.builder()
                        .name(abViewName)
                        .displayOrder(1)
                        .localizations(localz)
                        .build())
                .build());

        if (viewInfo == null)
            throw new Exception("Could not create store view");

        if (doPublish)
            publishStoreChange();

        viewId = viewInfo.getViewId();
        return viewId;
    }

    public List<SimpleItemInfo> createItems(int itemCount, String categoryPath, String itemDiff, boolean doPublish) throws Exception {
        if (storeId.equals(""))
            throw new Exception("No store id stored.");

        Item itemWrapper = new Item(abSdk);

        List<SimpleItemInfo> nItems = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {

            SimpleItemInfo nItemInfo = new SimpleItemInfo();
            nItemInfo.setTitle("Item " + itemDiff + " Titled " + Integer.toString(i + 1));
            nItemInfo.setSku("SKU_" + itemDiff + "_" + Integer.toString(i + 1));

            final Map<String, Localization> iLocalization = new HashMap<>();
            iLocalization.put("en",Localization.builder()
                    .title(nItemInfo.getTitle())
                    .build());

            final Map<String,List<RegionDataItem>> iRegionData = new HashMap<>();
            final List<RegionDataItem> regionItem = new ArrayList<>();
            regionItem.add(RegionDataItem.builder()
                    .currencyCode("USD")
                    .currencyNamespace("accelbyte")
                    .currencyTypeFromEnum(RegionDataItem.CurrencyType.REAL)
                    .price((i + 1) * 2)
                    .build());
            iRegionData.put("US",regionItem);

            final FullItemInfo newItem = itemWrapper.createItem(CreateItem.builder()
                    .namespace(abNamespace)
                    .storeId(storeId)
                    .body(ItemCreate.builder()
                            .name(nItemInfo.getTitle())
                            .itemTypeFromEnum(ItemCreate.ItemType.SEASON)
                            .categoryPath(categoryPath)
                            .entitlementTypeFromEnum(ItemCreate.EntitlementType.DURABLE)
                            .seasonTypeFromEnum(ItemCreate.SeasonType.TIER)
                            .statusFromEnum(ItemCreate.Status.ACTIVE)
                            .listable(true)
                            .purchasable(true)
                            .sku(nItemInfo.getSku())
                            .localizations(iLocalization)
                            .regionData(iRegionData)
                            .build())
                    .build());

            if (newItem == null)
                throw new Exception("Could not create store item");

            nItemInfo.setId(newItem.getItemId());
            nItems.add(nItemInfo);
        }

        if (doPublish)
            publishStoreChange();
        return nItems;
    }

    public SimpleSectionInfo createSectionWithItems(int itemCount, String categoryPath, boolean doPublish) throws Exception {
        if (storeId.equals(""))
            throw new Exception("No store id stored.");
        if (viewId.equals(""))
            throw new Exception("No view id stored.");

        final String itemDiff = getRandomString("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",6);

        List<SimpleItemInfo> items = createItems(itemCount,categoryPath,itemDiff,doPublish);
        List<SectionItem> sectionItems = new ArrayList<>();
        for (SimpleItemInfo item: items) {
            sectionItems.add(SectionItem.builder().id(item.getId()).build());
        }

        final String sectionTitle = (itemDiff + " Section");
        Map<String, Localization> localz = new HashMap<>();
        localz.put("en",Localization.builder()
                .title(sectionTitle)
                .build());

        final LocalDateTime now = LocalDateTime.now();
        final String sectionStart =
                now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        final String sectionEnd =
                now.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

        Section sectionWrapper = new Section(abSdk);
        FullSectionInfo newSection = sectionWrapper.createSection(CreateSection.builder()
                .namespace(abNamespace)
                .storeId(storeId)
                .body(SectionCreate.builder()
                        .viewId(viewId)
                        .displayOrder(1)
                        .name(sectionTitle)
                        .active(true)
                        .startDate(sectionStart)
                        .endDate(sectionEnd)
                        .rotationTypeFromEnum(SectionCreate.RotationType.FIXEDPERIOD)
                        .fixedPeriodRotationConfig(FixedPeriodRotationConfig.builder()
                                .backfillTypeFromEnum(FixedPeriodRotationConfig.BackfillType.NONE)
                                .ruleFromEnum(FixedPeriodRotationConfig.Rule.SEQUENCE)
                                .build())
                        .localizations(localz)
                        .items(sectionItems)
                        .build())
                .build());
        if (newSection == null)
            throw new Exception("Could not create new section");

        SimpleSectionInfo result = new SimpleSectionInfo();
        result.setId(newSection.getSectionId());
        result.setItems(items);

        if (doPublish)
            publishStoreChange();
        return result;
    }

    public void enableCustomRotationForSection(String sectionId, boolean doPublish) throws Exception {
        if (storeId.equals(""))
            throw new Exception("No store id stored.");

        Section sectionWrapper = new Section(abSdk);
        sectionWrapper.updateSection(UpdateSection.builder()
                .namespace(abNamespace)
                .storeId(storeId)
                .sectionId(sectionId)
                .body(SectionUpdate.builder()
                        .rotationTypeFromEnum(SectionUpdate.RotationType.CUSTOM)
                        .build())
                .build());

        if (doPublish)
            publishStoreChange();
    }

    public void enableFixedRotationWithCustomBackfillForSection(String sectionId, boolean doPublish) throws Exception {
        if (storeId.equals(""))
            throw new Exception("No store id stored.");

        Section sectionWrapper = new Section(abSdk);
        sectionWrapper.updateSection(UpdateSection.builder()
                .namespace(abNamespace)
                .storeId(storeId)
                .sectionId(sectionId)
                .body(SectionUpdate.builder()
                        .rotationTypeFromEnum(SectionUpdate.RotationType.FIXEDPERIOD)
                        .fixedPeriodRotationConfig(FixedPeriodRotationConfig.builder()
                                .backfillTypeFromEnum(FixedPeriodRotationConfig.BackfillType.CUSTOM)
                                .ruleFromEnum(FixedPeriodRotationConfig.Rule.SEQUENCE)
                                .build())
                        .build())
                .build());

        if (doPublish)
            publishStoreChange();
    }

    public void disableCustomFunctionForSection(String sectionId, boolean doPublish) throws Exception {
        if (storeId.equals(""))
            throw new Exception("No store id stored.");

        Section sectionWrapper = new Section(abSdk);
        sectionWrapper.updateSection(UpdateSection.builder()
                .namespace(abNamespace)
                .storeId(storeId)
                .sectionId(sectionId)
                .body(SectionUpdate.builder()
                        .rotationTypeFromEnum(SectionUpdate.RotationType.FIXEDPERIOD)
                        .fixedPeriodRotationConfig(FixedPeriodRotationConfig.builder()
                                .backfillTypeFromEnum(FixedPeriodRotationConfig.BackfillType.NONE)
                                .ruleFromEnum(FixedPeriodRotationConfig.Rule.SEQUENCE)
                                .build())
                        .build())
                .build());

        if (doPublish)
            publishStoreChange();
    }

    public void setPlatformServiceGrpcTarget() throws Exception {
        final String abGrpcServerUrl = config.getGrpcServerUrl();
        if (abGrpcServerUrl.equals(""))
            throw new Exception("Grpc Server Url is empty!");

        ServicePluginConfig wrapper = new ServicePluginConfig(abSdk);
        wrapper.updateServicePluginConfig(UpdateServicePluginConfig.builder()
                .namespace(abNamespace)
                .body(ServicePluginConfigUpdate.builder()
                        .grpcServerAddress(abGrpcServerUrl)
                        .build())
                .build());
    }

    public void unsetPlatformServiceGrpcTarget() throws Exception {
        ServicePluginConfig wrapper = new ServicePluginConfig(abSdk);
        wrapper.deleteServicePluginConfig(DeleteServicePluginConfig.builder()
                .namespace(abNamespace)
                .build());
    }

    public List<SimpleSectionInfo> getSectionRotationItems(String userId) throws Exception {
        if (viewId.equals(""))
            throw new Exception("No view id stored.");

        Section sectionWrapper = new Section(abSdk);

        List<SectionInfo> activeSections = sectionWrapper.publicListActiveSections(PublicListActiveSections.builder()
                .namespace(abNamespace)
                .viewId(viewId)
                .userId(userId)
                .build());
        if (activeSections == null)
            throw new Exception("Could not retrieve active sections data for current user.");

        List<SimpleSectionInfo> iSections = new ArrayList<>();
        for (SectionInfo section: activeSections) {
            List<ItemInfo> rItems = section.getCurrentRotationItems();
            SimpleSectionInfo sectionInfo = new SimpleSectionInfo();
            sectionInfo.setId(section.getSectionId());

            if ((rItems != null) && rItems.size() > 0) {
                List<SimpleItemInfo> items = new ArrayList<>();
                for (ItemInfo i: rItems) {
                    SimpleItemInfo item = new SimpleItemInfo();
                    item.setId(i.getItemId());
                    item.setSku(i.getSku());
                    item.setTitle(i.getTitle());
                    items.add(item);
                }
                sectionInfo.setItems(items);
            } else {
                sectionInfo.setItems(new ArrayList<>());
            }

            iSections.add(sectionInfo);
        }

        return iSections;
    }

    public void deleteStore() throws Exception {
        if (storeId.equals(""))
            return;

        Store storeWrapper = new Store(abSdk);
        storeWrapper.deleteStore(DeleteStore.builder()
                .namespace(abNamespace)
                .storeId(storeId)
                .build());
    }
}