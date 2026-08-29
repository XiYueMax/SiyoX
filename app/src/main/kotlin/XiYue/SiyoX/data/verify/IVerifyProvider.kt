// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.data.verify

data class VerifyResult(
    val success: Boolean,
    val message: String,
    val expireTime: Long = 0L
)

data class NoticeResult(
    val success: Boolean,
    val title: String,
    val content: String
)

interface IVerifyProvider {
    val providerName: String
    fun initProvider(androidId: String)
    fun verifyCard(cardKey: String, androidId: String, callback: (VerifyResult) -> Unit)
    fun fetchNotice(callback: (NoticeResult) -> Unit)
}
