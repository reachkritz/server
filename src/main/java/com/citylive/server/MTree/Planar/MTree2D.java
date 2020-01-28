package com.citylive.server.MTree.Planar;

import com.citylive.server.MTree.common.*;
import com.citylive.server.MTree.utils.Pair;
import com.citylive.server.MTree.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MTree2D extends MTree<Data> {
    private static final PromotionFunction<Data> nonRandomPromotion = new PromotionFunction<Data>() {
        @Override
        public Pair<Data> process(Set<Data> dataSet, DistanceFunction<? super Data> distanceFunction) {
            return Utils.minMax(dataSet);
        }
    };
    Map<String,Data> prevUserLoc;

    public MTree2D() {
        super(2,DistanceFunctions.EUCLIDEAN ,
                new ComposedSplitFunction<Data>(
                        nonRandomPromotion,
                        new PartitionFunctions.BalancedPartition<>()
                )
        );
        prevUserLoc = new TreeMap<>();
    }

    public List<ResultItem> getNearestByRangeAsList(Data queryData, double range) {
        return toList(super.getNearestByRange(queryData, range));
    }

    public List<ResultItem> getNearestByLimitAsList(Data queryData, int limit) {
        return toList(super.getNearestByLimit(queryData, limit));
    }

    public List<ResultItem> getNearestAsList(Data queryData, double range, int limit) {
        return toList(super.getNearest(queryData, range, limit));
    }

    public List<ResultItem> getNearestAsList(Data queryData) {
        return toList(super.getNearest(queryData));
    }

    public List<ResultItem> toList(Iterable<ResultItem> iterable){
        List<ResultItem> results = new ArrayList<>();
        for(Object ri : iterable){
            results.add((ResultItem) ri);
        }
        return results;
    }

    @Override
    public boolean remove(Data data) {
        prevUserLoc.remove(data.getId());
        return super.remove(data);
    }

    @Override
    public void add(Data data) {
        if(prevUserLoc.containsKey(data.getId())){
            this.remove(prevUserLoc.get(data.getId()));
        }
        super.add(data);
        prevUserLoc.put(data.getId(),data);

    }

}