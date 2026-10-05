/* No Windows, o SDL redefine main para SDL_main, que precisa de um
     * ponto de entrada proprio. Como o programa tem o seu proprio main,
     * avisamos o SDL que ja esta pronto antes de inicializar. */
    SDL_SetMainReady();