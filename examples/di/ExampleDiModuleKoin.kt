package com.company.app.feature.example.di

import com.company.app.feature.example.data.datasource.ExampleRemoteDataSource
import com.company.app.feature.example.data.repository.ExampleRepository
import com.company.app.feature.example.domain.repository.IExampleRepository
import com.company.app.feature.example.domain.usecase.GetExampleDataUseCase
import com.company.app.feature.example.presentation.viewmodel.ExampleViewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

// Koin Compiler Plugin DSL: constructors are wired and the graph is verified at build time.
// Needs the io.insert-koin.compiler.plugin Gradle plugin in this module (Kotlin >= 2.3.20).
// ApiClient comes from the app's network module.
val exampleModule = module {
    single<ExampleRemoteDataSource>()
    single<ExampleRepository>() bind IExampleRepository::class
    factory<GetExampleDataUseCase>()
    viewModel<ExampleViewModel>()
}
