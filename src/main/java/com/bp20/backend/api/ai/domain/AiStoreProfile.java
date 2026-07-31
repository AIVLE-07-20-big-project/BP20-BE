package com.bp20.backend.api.ai.domain;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사용자가 마지막으로 입력한 상권코드/업종코드를 캐싱해, 다음 업로드부터는 CSV만 보내도 되게 한다
@Getter
@Entity
@Table(name = "ai_store_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiStoreProfile extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "trdar_cd", nullable = false, length = 30)
    private String trdarCd;

    @Column(name = "svc_induty_cd", nullable = false, length = 30)
    private String svcIndutyCd;

    private AiStoreProfile(User user, String trdarCd, String svcIndutyCd) {
        this.user = user;
        this.trdarCd = trdarCd;
        this.svcIndutyCd = svcIndutyCd;
    }

    public static AiStoreProfile create(User user, String trdarCd, String svcIndutyCd) {
        return new AiStoreProfile(user, trdarCd, svcIndutyCd);
    }

    public void updateCodes(String trdarCd, String svcIndutyCd) {
        this.trdarCd = trdarCd;
        this.svcIndutyCd = svcIndutyCd;
    }
}
