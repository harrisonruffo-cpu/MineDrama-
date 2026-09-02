package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Drama
import com.example.ui.components.DramaCard
import com.example.ui.components.HeroBannerCarousel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DramaCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.DramaViewModel

@Composable
fun NovelasCatalogScreen(
    viewModel: DramaViewModel,
    onDramaSelected: (Drama) -> Unit,
    modifier: Modifier = Modifier
) {
    val dramas by viewModel.allDramas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val genres = listOf("Todos", "Romance", "Suspense", "Comédia", "Drama", "Ação", "Terror")
    var selectedGenre by remember { mutableStateOf("Todos") }

    val filteredDramas = remember(dramas, selectedGenre) {
        if (selectedGenre == "Todos") dramas
        else dramas.filter { it.genre.equals(selectedGenre, ignoreCase = true) }
    }

    val trendingDramas = remember(dramas) {
        dramas.filter { it.isTrending || it.isFeatured }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Banner Superior de Destaques
        if (trendingDramas.isNotEmpty()) {
            HeroBannerCarousel(
                dramas = trendingDramas,
                onDramaClick = onDramaSelected
            )
        }

        // Filtros de Categoria
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                val isSelected = genre == selectedGenre
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedGenre = genre },
                    label = {
                        Text(
                            text = genre,
                            color = if (isSelected) Color.White else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DramaCrimson,
                        containerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Grade de Dramas
        if (isLoading && dramas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DramaCrimson)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredDramas) { drama ->
                    DramaCard(
                        drama = drama,
                        onClick = { onDramaSelected(drama) }
                    )
                }
            }
        }
    }
}
