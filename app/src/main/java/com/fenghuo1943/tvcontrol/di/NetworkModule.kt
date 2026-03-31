package com.fenghuo1943.tvassistant.di

import com.fenghuo1943.tvassistant.input.InputSender
import com.fenghuo1943.tvassistant.network.TcpClient
import com.fenghuo1943.tvassistant.network.UdpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideUdpClient(): UdpClient = UdpClient()

    @Provides
    @Singleton
    fun provideTcpClient(): TcpClient = TcpClient()

    @Provides
    @Singleton
    fun provideInputSender(
        tcpClient: TcpClient,
        udpClient: UdpClient
    ): InputSender = InputSender(tcpClient, udpClient)
}