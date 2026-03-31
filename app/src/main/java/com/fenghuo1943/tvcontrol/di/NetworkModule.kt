package com.fenghuo1943.tvcontrol.di

import com.fenghuo1943.tvcontrol.input.InputSender
import com.fenghuo1943.tvcontrol.network.TcpClient
import com.fenghuo1943.tvcontrol.network.UdpClient
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