/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.fdmlib.dto.product.PublishedApiDTO;
import ru.beeline.fdmproducts.domain.mapic.ApiMapic;
import ru.beeline.fdmproducts.domain.mapic.CapabilityMapic;
import ru.beeline.fdmproducts.domain.mapic.ProductMapic;
import ru.beeline.fdmproducts.domain.mapic.PublishedApi;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.mapic.ApiMapicRepository;
import ru.beeline.fdmproducts.repository.mapic.CapabilityMapicRepository;
import ru.beeline.fdmproducts.repository.mapic.ProductMapicRepository;
import ru.beeline.fdmproducts.repository.mapic.PublishedApiRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MapicService {

    @Autowired
    ProductMapicRepository productMapicRepository;

    @Autowired
    CapabilityMapicRepository capabilityMapicRepository;

    @Autowired
    ApiMapicRepository apiMapicRepository;

    @Autowired
    PublishedApiRepository publishedApiRepository;

    public List<PublishedApiDTO> requestToMapic(String cmdb) {
        ProductMapic productMapic = productMapicRepository
                .findByCmdb(cmdb).orElseThrow(() -> new EntityNotFoundException("Запись с данным cmdb не найдена."));
        List<CapabilityMapic> capabilityMapicList = capabilityMapicRepository.findByProductId(productMapic);
        List<PublishedApiDTO> result = new ArrayList<>();
        if (!capabilityMapicList.isEmpty()) {
            List<ApiMapic> apiList = apiMapicRepository.findByCapabilityIdIn(capabilityMapicList);
            if (!apiList.isEmpty()) {
                List<PublishedApi> publishedApiList = publishedApiRepository.findAllByApiIdIn(apiList);
                if (!publishedApiList.isEmpty()) {
                    for (PublishedApi publishedApi : publishedApiList) {
                        result.add(PublishedApiDTO.builder()
                                .id(publishedApi.getId())
                                .apiContext(publishedApi.getContext())
                                .statusName(publishedApi.getStatus())
                                .apiId(publishedApi.getApiId().getId())
                                .build());
                    }
                }
            }
        }
        return result;
    }

    public String getMapicApi(Integer apiId) {
        ApiMapic apiMapic = apiMapicRepository
                .findById(apiId).orElseThrow(() -> new EntityNotFoundException("Запись с данным api-id не найдена."));
        return apiMapic.getSpec();
    }
}
