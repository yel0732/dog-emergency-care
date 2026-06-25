package com.ssafy.rescuemungz;

import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;

import static org.assertj.core.api.Assertions.assertThat;

class MapperScanConfigurationTests {
    @Test
    void mapperScanOnlyRegistersMapperAnnotatedInterfaces() {
        MapperScan mapperScan = RescueMungzApplication.class.getAnnotation(MapperScan.class);

        assertThat(mapperScan.annotationClass()).isEqualTo(Mapper.class);
    }
}
