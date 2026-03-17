import { createRouter, createWebHistory } from 'vue-router'
import ControlPanel from '../views/ControlPanel.vue'
import ConfigView from '../views/ConfigView.vue'
import ScoreboardView from '../views/ScoreboardView.vue'
import TournamentView from '../views/TournamentView.vue'
import IntroView from '../views/IntroView.vue'
import ReplayView from '../views/ReplayView.vue'
import PreviewView from '../views/PreviewView.vue'

const routes = [
  { path: '/', component: ControlPanel },
  { path: '/config', component: ConfigView },
  { path: '/preview', component: PreviewView },
  { path: '/scoreboard', component: ScoreboardView },
  { path: '/tournament', component: TournamentView },
  { path: '/intro', component: IntroView },
  { path: '/replay', component: ReplayView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
