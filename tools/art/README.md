# Offline wallpaper renderer

Renders the picture wallpapers (Space, Nature, Abstract, Bokeh, Night Highway) into
`app/src/main/assets/art/<style>/NN.webp`. Needs Python 3 with `numpy`, `scipy` and `Pillow`.

    python3 build_all.py                 # render what is missing
    python3 build_all.py --force         # re-render everything
    python3 build_all.py nebula          # only styles whose id contains "nebula"
    python3 preview.py space nebula 0.5 1 2 3   # half-size look, to /tmp/claude-0/art

`artlib.py` has the shared pieces (spectral noise, domain warping, bloom, a star field with
diffraction spikes, filmic tone mapping, and the finish that makes dark pixels exactly #000000).
Each module holds the generators for one world. `MANIFEST` in `build_all.py` lists the seeds
that are kept: a seed that makes a poor picture is simply left out.
