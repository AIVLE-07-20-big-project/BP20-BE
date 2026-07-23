package com.bp20.backend.weather.station;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AsosStationResolver {

    /**
     * 주요 ASOS 관측소 목록
     *
     * 초기 구현에서는 자주 사용되는 주요 지역을 등록합니다.
     * 이후 DB 또는 별도 CSV 파일로 전체 관측소 목록을 관리할 수 있습니다.
     */
    private static final List<AsosStation> STATIONS = List.of(
            new AsosStation("90", "속초", 38.2509, 128.5647),
            new AsosStation("93", "북춘천", 37.9474, 127.7544),
            new AsosStation("95", "철원", 38.1479, 127.3042),
            new AsosStation("98", "동두천", 37.9019, 127.0607),
            new AsosStation("99", "파주", 37.8859, 126.7665),
            new AsosStation("100", "대관령", 37.6771, 128.7183),
            new AsosStation("101", "춘천", 37.9026, 127.7357),
            new AsosStation("105", "강릉", 37.7515, 128.8909),
            new AsosStation("106", "동해", 37.5071, 129.1243),
            new AsosStation("108", "서울", 37.5714, 126.9658),
            new AsosStation("112", "인천", 37.4777, 126.6249),
            new AsosStation("114", "원주", 37.3375, 127.9466),
            new AsosStation("119", "수원", 37.2575, 126.9830),
            new AsosStation("121", "영월", 37.1813, 128.4574),
            new AsosStation("127", "충주", 36.9705, 127.9525),
            new AsosStation("129", "서산", 36.7766, 126.4939),
            new AsosStation("131", "청주", 36.6392, 127.4407),
            new AsosStation("133", "대전", 36.3719, 127.3721),
            new AsosStation("135", "추풍령", 36.2203, 127.9946),
            new AsosStation("136", "안동", 36.5729, 128.7073),
            new AsosStation("138", "포항", 36.0320, 129.3797),
            new AsosStation("140", "군산", 36.0053, 126.7614),
            new AsosStation("143", "대구", 35.8779, 128.6529),
            new AsosStation("146", "전주", 35.8409, 127.1172),
            new AsosStation("152", "울산", 35.5824, 129.3347),
            new AsosStation("156", "광주", 35.1729, 126.8916),
            new AsosStation("159", "부산", 35.1047, 129.0320),
            new AsosStation("165", "목포", 34.8173, 126.3815),
            new AsosStation("168", "여수", 34.7393, 127.7406),
            new AsosStation("184", "제주", 33.5141, 126.5297),
            new AsosStation("185", "고산", 33.2938, 126.1628),
            new AsosStation("188", "성산", 33.3868, 126.8802),
            new AsosStation("189", "서귀포", 33.2462, 126.5653)
    );

    /**
     * 주어진 위치에서 가장 가까운 ASOS 관측소를 반환합니다.
     */
    public AsosStation findNearestStation(double latitude, double longitude) {
        validateCoordinate(latitude, longitude);

        return STATIONS.stream()
                .min((station1, station2) -> {
                    double distance1 = calculateDistance(
                            latitude,
                            longitude,
                            station1.latitude(),
                            station1.longitude()
                    );

                    double distance2 = calculateDistance(
                            latitude,
                            longitude,
                            station2.latitude(),
                            station2.longitude()
                    );

                    return Double.compare(distance1, distance2);
                })
                .orElseThrow(() ->
                        new IllegalStateException("사용 가능한 ASOS 관측소가 없습니다.")
                );
    }

    /**
     * 위도·경도 유효성 검사
     */
    private void validateCoordinate(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException(
                    "위도는 -90 이상 90 이하여야 합니다."
            );
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    "경도는 -180 이상 180 이하여야 합니다."
            );
        }
    }

    /**
     * Haversine 공식을 사용한 두 좌표 사이 거리 계산
     *
     * 반환값 단위: km
     */
    private double calculateDistance(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {
        final double earthRadius = 6371.0;

        double latitudeDistance =
                Math.toRadians(latitude2 - latitude1);

        double longitudeDistance =
                Math.toRadians(longitude2 - longitude1);

        double a =
                Math.sin(latitudeDistance / 2)
                        * Math.sin(latitudeDistance / 2)
                        + Math.cos(Math.toRadians(latitude1))
                        * Math.cos(Math.toRadians(latitude2))
                        * Math.sin(longitudeDistance / 2)
                        * Math.sin(longitudeDistance / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        return earthRadius * c;
    }
}