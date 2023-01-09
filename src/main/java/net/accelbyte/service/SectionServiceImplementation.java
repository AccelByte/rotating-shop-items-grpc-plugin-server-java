package net.accelbyte.service;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import net.accelbyte.platform.catalog.section.v1.*;
import org.lognet.springboot.grpc.GRpcService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@GRpcService
public class SectionServiceImplementation extends SectionGrpc.SectionImplBase {

    private final float upperLimit = 24;

    @Override
    public void getRotationItems(GetRotationItemsRequest request, StreamObserver<GetRotationItemsResponse> responseObserver) {
        log.info("Received getRotationItems request");

        List<SectionItemObject> items = request.getSectionObject().getItemsList();
        final int inputCount = items.size();

        final float currentPoint = (float) LocalDateTime.now().getHour();
        final int selectedIndex = (int)Math.floor((inputCount/upperLimit)*currentPoint);

        SectionItemObject selectedItem = items.get(selectedIndex);
        List<SectionItemObject> responseItems = new ArrayList<>();
        responseItems.add(selectedItem);

        GetRotationItemsResponse response = GetRotationItemsResponse
                .newBuilder()
                .setExpiredAt(0)
                .addAllItems(responseItems)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void backfill(BackfillRequest request, StreamObserver<BackfillResponse> responseObserver) {
        log.info("Received backfill request");

        List<BackfilledItemObject> newItems = new ArrayList<>();
        for(RotationItemObject item : request.getItemsList()) {
            if (item.getOwned()) {
                //if and item is owned by user, then replace it with new item id.
                //item id will be generated randomly for example purpose.

                BackfilledItemObject newItem = BackfilledItemObject
                        .newBuilder()
                        .setItemId(UUID.randomUUID().toString().replace("-",""))
                        .setIndex(item.getIndex())
                        .build();
                newItems.add(newItem);
            }
        }

        BackfillResponse response = BackfillResponse
                .newBuilder()
                .addAllBackfilledItems(newItems)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
