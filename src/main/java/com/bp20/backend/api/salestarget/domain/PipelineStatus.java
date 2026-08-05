package com.bp20.backend.api.salestarget.domain;

/**
 * 영업 파이프라인 상태. 설계가이드 6.1 스펙 그대로.
 * CANDIDATE(신규 후보) -> CONTACT_PLANNED(연락예정) -> CONTACTED(접촉) -> MEETING(미팅)
 *   -> CONVERTED(전환) / HOLD(보류) / EXCLUDED(제외)
 */
public enum PipelineStatus {
    CANDIDATE,
    CONTACT_PLANNED,
    CONTACTED,
    MEETING,
    CONVERTED,
    HOLD,
    EXCLUDED
}
