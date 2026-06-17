package com.momobridge.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecurePrefs

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RegularPrefs
