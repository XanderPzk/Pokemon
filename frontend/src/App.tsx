import { Routes, Route } from 'react-router-dom';
import { Layout } from './components/ui';
import { LoginPage } from './features/auth/LoginPage';
import { RegisterPage } from './features/auth/RegisterPage';
import { RequireAuth } from './features/auth/RequireAuth';
import { MyLocalPokemonPage } from './features/pokemon/MyLocalPokemonPage';
import { PokemonDetailPage } from './features/pokemon/PokemonDetailPage';
import { PokemonListPage } from './features/pokemon/PokemonListPage';

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<PokemonListPage />} />
        <Route path="pokemon/:idOrName" element={<PokemonDetailPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route
          path="my-pokemon"
          element={
            <RequireAuth>
              <MyLocalPokemonPage />
            </RequireAuth>
          }
        />
      </Route>
    </Routes>
  );
}
