package CamNecT.server.domain.alumni.repository;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AlumniRepositoryCustom {

    /**
     * 동문 검색 조건에 따른 유저 ID 목록 조회
     *
     * @param myId 현재 사용자 ID (제외 대상)
     * @param name 검색할 이름 (옵션)
     * @param tagIdList 필터링할 태그 ID 목록 (옵션)
     * @return 조건에 맞는 유저 ID 목록 (공통 태그 수 DESC, 생성일 DESC 정렬)
     */
    List<Long> findAlumniIdsByConditions(Long myId, String name, List<Long> tagIdList, Pageable pageable);
}